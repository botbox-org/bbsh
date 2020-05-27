package com.botbox.bbsh;

import com.botbox.bbsh.jline.ConsoleCLIContext;
import org.jline.builtins.telnet.Telnet;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import java.io.PrintStream;
import java.util.Map;

public class Main {

    public static void main(String[] args) throws Exception {
        CLI cli = new CLI();

        /* Exec command is not among the default commands that are automatically registered */
        cli.registerCommand(ExecCommand.class);

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

        /* Also setup a telnet server */
        Terminal terminal = TerminalBuilder.builder().name("BBSH").build();
        Telnet telnet = new Telnet(terminal, new Telnet.ShellProvider() {
            @Override
            public void shell(Terminal terminal, Map<String, String> environment) {
                CLIContext cliContext2;
                terminal.echo(true);
                System.out.println("Shell Terminal:" + terminal.echo(true));
                cliContext2 = new StreamCLIContext(cli, terminal.input(), new PrintStream(terminal.output()), new PrintStream(terminal.output()));
                cliContext2.setPrompt("bbsh>");
                cliContext2.start();
                for (;;) {
                    try {
                        Thread.sleep(60000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        telnet.telnetd(new String[] {"telnetd", "-p", "2700", "start"});

        for (;;) {
            try {
                System.out.println("sleep...");
                Thread.sleep(60000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
