package io.milton.sync;

import io.milton.common.FileUtils;
import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * For removing old versions of local files when they're updated. 
 * 
 * This bit is quite sensitive, we really can't afford to eat anyone's files!
 * 
 * So this implementation is very defensive, it never deletes files, just moves
 * them into a versions folder
 *
 * @author brad
 */
public class Archiver {

    private static final Logger log = LoggerFactory.getLogger(Archiver.class);
    
    public Archiver() {
    }
    
    
    
    public void archive(File f) {
        File versions = getVersionsDir(f);
        File dest = new File(versions, f.getName());
        String name = FileUtils.incrementFileName(dest.getName(), true);
        while(dest.exists()) {
            name = FileUtils.incrementFileName(name, false);
            dest = new File(versions, name);
        }
        if( !f.renameTo(dest) ) {
            throw new RuntimeException("Couldnt archive old file: " + f.getAbsolutePath() + " to: " + dest.getAbsolutePath() );
        }
    }
    
    private File getVersionsDir(File contentFile) {
        File hiddenDir = new File(contentFile.getParent(), ".spliffy");
        if (!hiddenDir.exists()) {
            log.info("getVersionsDir: Creating missing hidden dir", hiddenDir.getAbsolutePath());
            if (!hiddenDir.mkdirs()) {
                throw new RuntimeException("Couldnt create directory: " + hiddenDir.getAbsolutePath());
            }
        }
        File versionsDir = new File(hiddenDir, "versions");
        if( !versionsDir.exists()) {
            log.info("getVersionsDir: Creating missing versions dir", versionsDir.getAbsolutePath());
            if(!versionsDir.mkdirs() ) {
                throw new RuntimeException("Couldnt create directory for old versions: " + versionsDir.getAbsolutePath());
            }
        }
        return versionsDir;
    }    
}
