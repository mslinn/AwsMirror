package com.micronautics.aws;

import akka.dispatch.Futures;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.regex.Pattern;

import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

public class DirectoryWatcher {
    private static final Logger logger = LoggerFactory.getLogger(DirectoryWatcher.class);
    private WatchService watcher = null;
    private final Path dir;
    private final String rootDir;
    private final int rootDirLen;
    private final Map<WatchKey, Path> keys = new HashMap<>();
    private final Timer timer = new Timer();

    /** Time by which all file operations should have settled */
    protected long debounceTime = 10 * 1000L; // todo make this configurable

    /** Milliseconds between checks for debounced file events */
    protected long debounceCheckInterval = 250; // todo make this configurable

    protected HistoryMap historyMap = new HistoryMap();

    protected DebounceQueue debounceQueue = new DebounceQueue();
    protected QueueTask queueTask = new QueueTask();


    /** On Windows, using DreamWeaver, ENTRY_CREATE was always followed by one to 3 ENTRY_MODIFY (in testing) -
     * up to 4.5 seconds afterwards! */
    public DirectoryWatcher(Path watchedPath) throws IOException{
        this.dir = watchedPath;
        this.rootDir = dir.toFile().getAbsolutePath();
        this.rootDirLen = rootDir.length();
        this.watcher = FileSystems.getDefault().newWatchService();
        for (Path path : subPaths(watchedPath, new ArrayList<Path>())) {
            WatchKey watchKey = path.register(watcher, /*ENTRY_CREATE, */ENTRY_DELETE, ENTRY_MODIFY);
            Path relativePath = watchedPath.relativize(path);
            keys.put(watchKey, relativePath);
            logger.debug("watchKey " + watchKey + " => " + relativePath.toString());
        }
        timer.schedule(queueTask, debounceCheckInterval, debounceCheckInterval);
    }

    protected static boolean ignore(File file) {
        for (Pattern pattern : Model.ignoredPatterns)
            if (pattern.matcher(file.getName()).matches()) {
                System.out.println("DirectoryWatcher ignoring " + file.getName());
                return true;
            }
        return false;
    }

    protected static ArrayList<Path> subPaths(Path path, ArrayList<Path> result) {
        logger.debug("  Watching subPath " + path);
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
        for (;;) {
            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException x) {
                return;
            }

            Path basePath = keys.get(key);
            logger.debug("key=" + key + "; basePath=" + basePath);

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                @SuppressWarnings("unchecked") WatchEvent<Path> ev = (WatchEvent<Path>) event;
                Path path = ev.context();

                String relativePath = "";
                if (basePath.toString().length()>0)
                    relativePath = basePath.toString() + "/";
                relativePath += path.toString();
                relativePath = relativePath.replace("\\", "/");

                logger.debug("path=" + path + "; relativePath=" + relativePath);

                if (kind == OVERFLOW) { // were events lost or discarded?
                    logger.warn("DirectoryWatcher got an OVERFLOW event for " + relativePath);
                    continue;
                }
                if (ignore(path.toFile())) {
                    logger.debug("DirectoryWatcher ignoring '" + path.getFileName() + "'");
                    continue;
                }
                historyMap.update(relativePath, event);
            }

            // Reset the key -- important in order to receive further watch events.
            // If the key is no longer valid, the directory is inaccessible so exit the loop.
            if (!key.reset())
                break;
        }
    }

    /** @param time milliseconds */
    protected static long roundToNearestSecond(long time) { return (time / 1000L) * 1000L; }

    private class QueueTask extends TimerTask {
        /** Check queue for events old enough to take action on */
        public void run() {
            logger.debug("Entering QueueTask.run(); debounceQueue has " + debounceQueue.size() + " items in it.");
            while (debounceQueue.peek()!=null && debounceQueue.peek().isDebounced()) {
                logger.debug("About to debounceQueue.poll()");
                FileHistory fileHistory = debounceQueue.poll();
                logger.debug("About to dir.relativize(); dir=" + dir + "; fileHistory.getFile().toPath()=" + fileHistory.getFile().toPath());
                Path relativePath = fileHistory.getFile().toPath();
                logger.debug("Popped off debounceQueue: " + fileHistory + "; relativePath=" + relativePath);
                if (fileHistory.getFile().exists()) {
                    logger.debug("DirectoryWatcher uploading '" + relativePath + "' from AWS S3.");
                    String s3Key = relativePath.toString();
                    if (Model.multithreadingEnabled)
                        Futures.future(new UploadOne(s3Key, fileHistory.getFile()), Main.system().dispatcher());
                    else
                        new UploadOne(s3Key, fileHistory.getFile()).call();
                } else {
                    logger.debug("DirectoryWatcher deleting '" + relativePath + "' from AWS S3.");
                    Model.s3.deleteObject(Model.bucketName, relativePath.toString());
                }
                historyMap.remove(relativePath);
                logger.debug("Removed '" + relativePath + "' from historyMap; size=" + historyMap.size());
            }
        }
    }

    protected class DebounceQueue extends PriorityBlockingQueue<FileHistory> {}

    private class HistoryMap extends ConcurrentHashMap<Path, FileHistory> {
        public void update(String relativePath, WatchEvent<?> event) {
            logger.debug("HistoryMap.update; ");
            Path path = Paths.get(relativePath);
            FileHistory fileHistory = get(path);
            if (fileHistory==null)
                fileHistory = new FileHistory(path);
            FileEvent fileEvent = new FileEvent(path, event);
            fileHistory.events.add(fileEvent);
            logger.debug("Added fileEvent to fileHistory.events with time=" + fileEvent.getTime());
            put(path, fileHistory);
            logger.debug("Put fileHistory to HistoryMap");
            if (!debounceQueue.contains(fileHistory))
                debounceQueue.add(fileHistory);
        }
    }

    /** History and present status of file that has recently had one or more FileEvents.
     * Sort order is FileHistory with oldest event at tail of the queue first. */
    protected class FileHistory implements Comparable<FileHistory> {
        private ConcurrentLinkedDeque<FileEvent> events = new ConcurrentLinkedDeque<>();
        private Path path;
        private File file;

        public FileHistory(Path path) {
            this.path = path;
            this.file = path.toFile();
        }

        @Override
        public int compareTo(FileHistory that) {
            long thisTime = this.events.peekLast().getTime();
            long thatTime = that.events.peekLast().getTime();
            if (thisTime<thatTime)
                return -1;
            if (thisTime>thatTime)
                return 1;
            return 0;
        }

        /** @return last modified time, in milliseconds, or 0 if the file does not exist.
         * Time is rounded to the nearest second for consistency on all OSes.
         * Once uploaded to S3, we need the same behavior on all OSes, and some OSes only support second resolution. */
        public long getLastModified() {
            if (file.exists())
              return roundToNearestSecond(file.lastModified());
            return 0;
        }

        public File getFile() { return file; }

        public Path getPath() { return path; }

        public boolean isDebounced() {
            FileEvent[] eventArray = events.toArray(new FileEvent[events.size()]); // get consistent view
            int n = eventArray.length;
            long lastEventTime = eventArray[n-1].getTime();
            long now = System.currentTimeMillis();
            if (now - lastEventTime >= debounceTime) {
                logger.debug("isDebounced returning true because of no activity for " + (now - lastEventTime) + " ms");
                return true;
            }
            return false;
        }
    }

    /** Record of something happening to a file (delete, create, modify). */
    protected class FileEvent {
        private WatchEvent<?> watchEvent;

        /** Timestamp of event, in milliseconds, without rounding */
        private long time = System.currentTimeMillis();
        private Path path;

        /** @param path is fully qualified Path of file referenced by watchEvent */
        public FileEvent(Path path, WatchEvent<?> watchEvent) {
            this.path = path;
            this.watchEvent = watchEvent;
        }

        public File getFile() { return path.toFile(); }

        /** @return last modified time, in milliseconds, rounded to the nearest second for consistency on all OSes.
         * Once uploaded to S3, we need the same behavior on all OSes, and some OSes only support second resolution. */
        public long getLastModified() { return roundToNearestSecond(path.toFile().lastModified()); }

        public Path getPath() { return path; }

        public long getTime() { return time; }

        public WatchEvent.Kind<?> getKind() { return watchEvent.kind(); }

        public WatchEvent<?> getWatchEvent() { return watchEvent; }
    }
}
