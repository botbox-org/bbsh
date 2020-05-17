/**
 * Copyright (c) 2008-2016, Swedish Institute of Computer Science.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
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
 * ExecCommand
 *
 * Author  : Joakim Eriksson, Niclas Finne
 * Created : Sun Mar 09 23:15:36 2008
 */
package com.botbox.bbsh;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import org.kohsuke.args4j.Argument;

@CLICommand(name="exec", description="execute an external command")
public class ExecCommand extends BasicLineCommand {

    @Argument(metaVar = "[arg [arg2 [arg3] ...]]", usage = "arguments")
    private List<String> arguments = new ArrayList<String>();

    private CommandContext context;
    private Process process;
    private Runner runner;
    private Reader stdout;
    private Reader stderr;
    private PrintStream output;

    @Override
    public int executeCommand(CommandContext context) {
        if (arguments.isEmpty()) {
            context.err.println("no command to execute");
            return 1;
        }

        String[] args = arguments.toArray(new String[arguments.size()]);
        String name = args[0];
        this.context = context;

        try {
            process = Runtime.getRuntime().exec(args);
            stdout = new Reader(name + ".out", this, process.getInputStream(),
                    context.out);
            stdout.start();
            stderr = new Reader(name + ".err", this, process.getErrorStream(),
                    context.err);
            stderr.start();

            output = new PrintStream(process.getOutputStream());

            runner = new Runner(name + ".proc", this);
            runner.start();
        } catch (Exception e) {
            context.err.println("failed to start command: " + e.getMessage());
            return 1;
        }
        return 0;
    }

    @Override
    public void lineRead(String line) {
        System.out.println("Exec: sending a line: " + line);
        output.println(line);
        output.flush();
    }

    @Override
    public void stopCommand(CommandContext context) {
        if (runner != null && runner.isRunning) {
            process.destroy();
        } else {
            context.exit(0);
        }
    }

    // -------------------------------------------------------------------
    // Handler for the executer
    // -------------------------------------------------------------------

    private static class Runner extends Thread {

        public ExecCommand command;
        public boolean isRunning;

        public Runner(String name, ExecCommand command) {
            super(name);
            this.command = command;
            this.isRunning = false;
        }

        public void run() {
            int exitValue = -1;
            isRunning = true;
            try {
                command.process.waitFor();

                // Wait for the readers to be finished before reporting about
                // the process exit
                for (int i = 0; i < 5
                        && (command.stdout.isAlive() || command.stderr
                                .isAlive()); i++) {
                    Thread.sleep(300);
                }

                exitValue = command.process.exitValue();

            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                isRunning = false;
                command.context.exit(exitValue);
            }
        }

    } // end of inner class Runner

    // -------------------------------------------------------------------
    // Handler for reading the input streams
    // -------------------------------------------------------------------

    private static class Reader extends Thread {

        private ExecCommand command;
        private PrintStream out;
        private BufferedReader input;

        public Reader(String name, ExecCommand command, InputStream input,
                PrintStream out) {
            super(name);
            this.command = command;
            this.input = new BufferedReader(new InputStreamReader(input));
            this.out = out;
        }

        public void run() {
            try {
                String line;
                while ((line = input.readLine()) != null) {
                    out.println(line);
                }
            } catch (Exception e) {
                command.context.err.println("* " + getName() + " failed: "
                        + e.getMessage());
            }
        }

    } // end of inner class Reader

}
