package com.micronautics.aws;

import akka.dispatch.Futures;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.regex.Pattern;

import static java.nio.file.StandardWatchEventKinds.*;

public class DirectoryWatcher {
    private Logger logger = LoggerFactory.getLogger(getClass());
    private WatchService watcher = null;
    private final Path dir;

    /** ENTRY_CREATE was always followed by one to 3 ENTRY_MODIFY (in testing) - up to 4.5 seconds afterwards!
     * This called for some crazy login in watch(). */
    public DirectoryWatcher(Path watchedPath) throws IOException{
        this.watcher = FileSystems.getDefault().newWatchService();
        for (Path path : subPaths(watchedPath, new ArrayList<Path>())) {
            logger.debug("Watching " + path.toString());
            path.register(watcher, /*ENTRY_CREATE, */ENTRY_DELETE, ENTRY_MODIFY);
        }
        this.dir = watchedPath;
    }

    protected boolean ignore(File file) {
        for (Pattern pattern : Model.ignoredPatterns)
            if (pattern.matcher(file.getName()).matches()) {
                System.out.println("DirectoryWatcher ignoring " + file.getName());
                return true;
            }
        return false;
    }

    protected ArrayList<Path> subPaths(Path path, ArrayList<Path> result) {
        if (path.toFile().isDirectory() && result.size()==0)
            result.add(path);

        for (File file : path.toFile().listFiles())
            if (file.isDirectory()) {
                Path subPath = file.toPath();
                result.add(subPath);
                subPaths(subPath, result);
            }
        return result;
    }

    /** Process all events for the key queued to the watcher. */
    public void watch() {
        Path lastPath = null;
        long lastTime = new DateTime().getMillis();
        long lastModified = 0L;
        for (;;) {
            // wait for key to be signaled
            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException x) {
                return;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();

                // An OVERFLOW event occurs when events are lost or discarded.
                if (kind == OVERFLOW)
                    continue;

                // The filename is the context of the event.
                WatchEvent<Path> ev = (WatchEvent<Path>) event;
                Path path = ev.context();

                if (ignore(path.toFile())) {
                    logger.debug("DirectoryWatcher ignoring '" + path.getFileName() + "'");
                    return;
                }

                long thisTime = new DateTime().getMillis();
                long dt = thisTime - lastTime;

                if (kind==ENTRY_DELETE) {
                    lastModified = 0L;
                    lastPath = path;
                    lastTime = thisTime;

                    logger.debug("TODO: take action on '" + path + "'; " + kind + "; dt=" + dt + "ms");
                    // todo delete from S3
                    continue;
                }

                boolean differentFile = lastPath==null || path.compareTo(lastPath)!=0;
                if (!differentFile && lastModified==path.toFile().lastModified() && kind==ENTRY_MODIFY) {
                    logger.debug("DirectoryWatcher skipping '" + path + "'; " + kind + "; lastModified=" + lastModified);
                    continue;
                }

                if (differentFile || (!differentFile && dt>150)) {
                    logger.debug("DirectoryWatcher starting upload of '" + path + "'; " + kind + "; dt=" + dt + "ms");
                    String s3Key = path.toString(); // might need to make this relative to watchedPath
                    Futures.future(new UploadOne(s3Key, path.toFile()), Main.system().dispatcher());
                } else {
                    //System.out.println("Skipping duplicate " + path + "; " + kind + "; dt=" + dt);
                }
                lastPath = path;
                lastTime = thisTime;
                lastModified = path.toFile().lastModified();

                // Resolve the filename against the directory.
                // If the filename is "test" and the directory is "foo", the resolved name is "test/foo".
//                    Path child = dir.resolve(filename);
//                    if (!Files.probeContentType(child).equals("text/plain")) {
            }
            // Reset the key -- this step is critical if you want to receive further watch events.
            // If the key is no longer valid, the directory is inaccessible so exit the loop.
            boolean valid = key.reset();
            if (!valid)
                break;
        }
    }
}
