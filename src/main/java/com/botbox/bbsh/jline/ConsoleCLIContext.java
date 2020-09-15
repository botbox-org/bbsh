/*
 * Copyright (c) 2016, Swedish Institute of Computer Science.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the Institute nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE INSTITUTE AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 * -----------------------------------------------------------------
 *
 * ConsoleCLIContext
 *
 * Authors : Joakim Eriksson, Niclas Finne
 */

package com.botbox.bbsh.jline;

import org.jline.builtins.Completers;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.botbox.bbsh.CLI;
import com.botbox.bbsh.CLIContext;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 *
 */
public class ConsoleCLIContext extends CLIContext {

    private static final Logger log = LoggerFactory.getLogger(ConsoleCLIContext.class);

    private LineReader console;
    private boolean exit = false;
    private boolean hasStarted = false;

    public ConsoleCLIContext(CLI cli) {
        super(cli, new ConsolePrintStream(), new ConsolePrintStream());
    }

    @Override
    public void setPrompt(String prompt) {
        super.setPrompt(prompt);
    }

    @Override
    public void start() {
        if (hasStarted) {
            return;
        }
        hasStarted = true;
        Thread t = new Thread(new Runnable() {
            @Override public void run() {
                try {
                    console = LineReaderBuilder.builder().completer(new Completers.FileNameCompleter()).build();
                    ((ConsolePrintStream)out).setConsoleReader(console);
                    ((ConsolePrintStream)err).setConsoleReader(console);
                    String line;
                    while (!exit && (line = console.readLine(prompt)) != null) {
                        line = line.trim();
                        if (line.length() > 0 && !line.startsWith("#")) {
                            executeCommand(line);
                        }
                    }
                } catch (Exception e) {
                    log.warn("CLI console input died!", e);
                } finally {
                    // nothing?
                }
                log.info("Console reader exited");
                System.exit(0);
            }
        }, "cmd");
        t.setDaemon(false);
        t.start();
    }

    private static class ConsolePrintStream extends PrintStream {

        public ConsolePrintStream() {
            super(new ConsoleOutputStream());
        }

        public void setConsoleReader(LineReader console) {
            ((ConsoleOutputStream)out).setConsoleReader(console);
        }

    }

    @Override
    protected String readLine() throws IOException {
        return console.readLine();
    }

    private static class ConsoleOutputStream extends OutputStream {

        private StringBuilder line = new StringBuilder();
        private LineReader console;

        @Override
        public void write(int c) throws IOException {
            if (c == '\n') {
                if (console != null) {
                    console.printAbove(line.toString());
                }
                line.setLength(0);
            } else if (c != '\r'){
                line.append((char) c);
            }
        }

        public void setConsoleReader(LineReader console) {
            this.console = console;
        }

    }
}
