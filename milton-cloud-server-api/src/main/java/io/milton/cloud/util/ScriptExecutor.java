/*
 * Copyright (C) 2012 McEvoy Software Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.cloud.util;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import javax.script.ScriptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Use this to execute a native script file, such as a bash script on
linux or a batch file on windows
 *
 * The exec method blocks until the script has terminated. Success is
determined by the script return code. Information
 * returned by the script is buffered and will be added to an exception
if thrown to support logging
 *
 * This class explicitly supports win32 batch files. If
setWin32Batch(true) is called then the process argument is assumed to
 * be a batch file and cmd.exe /c is inserted into the called command.
 *
 * @author mcevoyb
 *
 */
public class ScriptExecutor {

    private static final Logger log = LoggerFactory.getLogger(com.bradmcevoy.common.ScriptExecutor.class);
    
    private final String process;
    private final String[] args;
    private final int successCode;
    private boolean win32Batch;

    private String output;
    
    /**
     *
     * @param process - name and/or path of the script file. May be a
    win32 batch file
     * @param args - array of string arguments to be set on the command
    line
     * @param successCode - numeric code which will indicate successful
    completion. This is normally 0
     */
    public ScriptExecutor(String process, List<String> args, int successCode) {
        this.process = process;
        this.args = new String[args.size()];
        args.toArray(this.args);
        this.successCode = successCode;
        if (process.endsWith(".bat")) {
            setWin32Batch(true);
        }  
    }

    public boolean isWin32Batch() {
        return win32Batch;
    }

    /**
     * Set this to true to force the class to format the command to
    execute cmd.exe /c process {args..}
     *
     * @param win32Batch
     */
    public void setWin32Batch(boolean win32Batch) {
        this.win32Batch = win32Batch;
    }

    /**
     * Synchronously executes the script file. Once complete, the return
    code will be inspected for success
     *
     * @throws ScriptException -
     */
    public void exec() throws Exception {
        log.info("exec: " + process);
        int cmdSize = args.length + 1;
        if (isWin32Batch()) {
            cmdSize += 2;
        }
        String[] cmd = new String[cmdSize];
        int param = 0;
        if (isWin32Batch()) {
            cmd[param++] = "cmd.exe";
            cmd[param++] = "/c";
        }
        cmd[param++] = process;
        for (int i = 0; i < args.length; i++) {
            String s = args[i];
            if (s == null) {
                s = "";
            }
            cmd[param++] = s;
        }
        log.info("cmd= " + formatCommand(cmd));
        Runtime rt = Runtime.getRuntime();
        try {
            Process proc = rt.exec(cmd);
            ScriptExecutor.ScriptOutputReader errorDiscarder = new ScriptExecutor.ScriptOutputReader(proc.getErrorStream());
            ScriptExecutor.ScriptOutputReader output = new ScriptExecutor.ScriptOutputReader(proc.getInputStream());
            debug("starting process.");            
            errorDiscarder.start();
            output.start();
            debug("...waiting for proc...");
            int exitVal = proc.waitFor();
            debug("...got exit val: " + exitVal);
            if (exitVal != successCode) {
                log.error("error output: "  + errorDiscarder.toString());
                log.error("Command=" + formatCommand(cmd));
                throw new Exception(exitVal + " - " + output.toString());
            }
            debug("...waiting for threads to join...");
            output.join(10000); // 10 sec at most!
            errorDiscarder.join(1000);  // 1 more sec at most!
            if (output.isAlive()) {
                output.interrupt();
            }
            this.output = output.toString();
            if (errorDiscarder.isAlive()) {
                errorDiscarder.interrupt();
            }
            debug("...done ok");
        } catch (IOException ioe) {
            throw new Exception(ioe);
        } catch (InterruptedException ie) {
            throw new Exception(ie);
        } finally {
            log.debug("finished exec");
        }
    }

    private String formatCommand(String[] cmd) {
        StringBuilder sb = new StringBuilder();
        for( String s : cmd ) {
            sb.append(s).append(" ");
        }
        return sb.toString();
    }

    private abstract static class StreamReader extends Thread {

        private InputStream is;

        protected abstract void processLine(String line);

        public StreamReader(InputStream is) {
            this.is = is;
        }

        @Override
        public void run() {
            try {
                InputStreamReader isr = new InputStreamReader(is);
                try (BufferedReader br = new BufferedReader(isr)) {
                    String line = null;
                    while ((line = br.readLine()) != null) {
                        if (interrupted()) {
                            break;
                        }
                        processLine(line);
                    }
                }
            } catch (IOException ioe) {
                log.error("Exception in StreamReader", ioe);
            }
        }
    }

    private static class StreamDiscarder extends ScriptExecutor.StreamReader {

        public StreamDiscarder(InputStream is) {
            super(is);
        }

        @Override
        protected void processLine(String line) {
            log.trace(line);
        }
    }

    private static class ScriptOutputReader extends ScriptExecutor.StreamReader {

        private StringBuffer sb = new StringBuffer();

        public ScriptOutputReader(InputStream is) {
            super(is);
        }

        @Override
        protected void processLine(String cmdOut) {
            sb.append(cmdOut).append("\n");
            log.trace(cmdOut);
        }

        @Override
        public String toString() {
            return sb.toString();
        }
    }
    
    private static void debug(Object o) {
        log.debug(o.toString());
    }

    @Override
    public String toString() {
        String s = "command: " + process + " arbuments:";
        for(String arg : args) {
            s += arg + ",";
        }
        return s;
    }

    public String getOutput() {
        return output;
    }
    
    
}
