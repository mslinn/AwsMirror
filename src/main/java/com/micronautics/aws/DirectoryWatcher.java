package com.micronautics.aws;

import org.joda.time.DateTime;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

// todo marked abstract for upload and delete handlers
public class DirectoryWatcher {
    private WatchService watcher = null;
    private final Path dir;

    /** ENTRY_CREATE was always followed by one to 3 ENTRY_MODIFY (in testing) - up to 4.5 seconds afterwards!
     * This called for some crazy login in watch(). */
    public DirectoryWatcher(Path watchedPath) throws IOException{
        this.watcher = FileSystems.getDefault().newWatchService();
        watchedPath.register(watcher, /*ENTRY_CREATE, */ENTRY_DELETE, ENTRY_MODIFY);
        this.dir = watchedPath;
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

                long thisTime = new DateTime().getMillis();
                long dt = thisTime - lastTime;

                // The filename is the context of the event.
                WatchEvent<Path> ev = (WatchEvent<Path>) event;
                Path path = ev.context();
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
                    System.out.println("Skipping " + path + "; " + kind + "; lastModified=" + lastModified);
                    continue;
                }

                if (differentFile || (!differentFile && dt>150)) {
                    System.out.println("Take action on " + path + "; " + kind + "; dt=" + dt + "ms");
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
