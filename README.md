# bbsh

Minimall Shell for Java applications to enable runtime configuration, control. querying, etc.

# defining new shell commands:

Implementing a basic echo command is done as follows:


    @CLICommand(name="echo", topic="core", description="echo arguments")
    public static class EchoCommand implements Command {

        @Argument(metaVar = "[arg [arg2 [arg3] ...]]", usage = "arguments")
        private List<String> args = new ArrayList<String>();

        @Override
        public int executeCommand(CommandContext context) throws CLIException {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, n = args.size(); i < n; i++) {
                if (i > 0) sb.append(' ');
                sb.append(args.get(i));
            }
            context.out.println(sb.toString());
            return 0;
        }
    }

