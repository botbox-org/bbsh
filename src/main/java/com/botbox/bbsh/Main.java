package com.botbox.bbsh;

import com.botbox.bbsh.jline.ConsoleCLIContext;

public class Main {

    public static void main(String[] args) {
        CLI cli = new CLI();
        CLIContext cliContext;
        boolean useJlineFallback = false;
        if (useJlineFallback && "xterm".equalsIgnoreCase(System.getenv("TERM"))) {
            // Special case - do not use jline2!
            System.err.println("*** fallback to simple input");
            cliContext = new StreamCLIContext(cli, System.in, System.out, System.err);
        } else {
            cliContext = new ConsoleCLIContext(cli);
        }
        cliContext.setPrompt("bbsh> ");
        cliContext.start();

        for (;;) {
            try {
                Thread.sleep(60000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
