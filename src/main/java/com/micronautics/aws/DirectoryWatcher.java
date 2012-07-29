package com.micronautics.aws;

import org.joda.time.DateTime;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.regex.Pattern;

import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

// todo mark abstract for upload and delete handlers
public class DirectoryWatcher {
    private WatchService watcher = null;
    private final Path dir;
    private List<Pattern> ignoredPatterns;

    /** ENTRY_CREATE was always followed by one to 3 ENTRY_MODIFY (in testing) - up to 4.5 seconds afterwards!
     * This called for some crazy login in watch(). */
    public DirectoryWatcher(Path watchedPath, List<Pattern> ignoredPatterns) throws IOException{
        this.watcher = FileSystems.getDefault().newWatchService();
        watchedPath.register(watcher, /*ENTRY_CREATE, */ENTRY_DELETE, ENTRY_MODIFY);
        this.dir = watchedPath;
        this.ignoredPatterns = ignoredPatterns;
    }

    protected boolean ignore(File file) {
        for (Pattern pattern : ignoredPatterns)
            if (pattern.matcher(file.getName()).matches()) {
                System.out.println("DirectoryWatcher ignoring " + file.getName());
                return true;
            }
        return false;
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
                    System.out.println("DirectoryWatcher ignoring " + path.getFileName());
                    return;
                }

                long thisTime = new DateTime().getMillis();
                long dt = thisTime - lastTime;

                if (kind==ENTRY_DELETE) {
                    lastModified = 0L;
                    lastPath = path;
                    lastTime = thisTime;

                    System.out.println("Take action on " + path + "; " + kind + "; dt=" + dt + "ms");
                    // todo delete from S3
                    continue;
                }

                boolean differentFile = lastPath==null || path.compareTo(lastPath)!=0;
                if (!differentFile && lastModified==path.toFile().lastModified() && kind==ENTRY_MODIFY) {
                    System.out.println("DirectoryWatcher skipping " + path + "; " + kind + "; lastModified=" + lastModified);
                    continue;
                }

                if (differentFile || (!differentFile && dt>150)) {
                    System.out.println("DirectoryWatcher should take action on " + path + "; " + kind + "; dt=" + dt + "ms");
                    // todo Futures.future(new UploadOne(bucketName, path, file), dispatcher);
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
