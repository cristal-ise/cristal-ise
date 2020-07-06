package org.cristalise.dev.scaffold

import org.cristalise.kernel.entity.proxy.AgentProxy
import org.cristalise.kernel.process.AbstractMain
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.process.StandardClient

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

@CompileStatic
class Generator extends StandardClient {
    
    String config
    String connect
    Integer logLevel = 0

    public void main(String[] args) {
        def g = new Generator()

        if (!g.readArgs(args)) return

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                AbstractMain.shutdown(0);
            }
        });

        Gateway.init(readPropertyFiles(config, connect, null))
        
        if (authenticate()) {
            
        }

        shutdown(0)
    }

    @CompileDynamic
    private boolean readArgs(String[] args) {
        def cli = new CliBuilder(usage: 'Generator -[hcsl]')
        cli.width = 100

        cli.with {
            h longOpt: 'help', 'Show usage information'
            c longOpt: 'config', args: 1, argName: 'config.conf', 'Cristal-ise config file'
            s longOpt: 'connect', args: 1, argName: 'server.clc', 'Cristal-ise config file'
            l longOpt: 'logLevel', args: 1, argName: 'logLevel', 'Set cristal-ise log level [0-9] '
            //d longOpt: 'debug', 'Print extra information'
        }

        def options = cli.parse(args)

        // Show usage text when error or -h or --help option is used.
        if (!args || !options || options.h) {
            cli.usage(); return false
        }

//        if (!options.arguments()) {
//            println "Please provide input csv file"
//            cli.usage()
//            return false
//        }

        if (!options.c || !options.s) {
            println "Please provide --config and --connect files"
            cli.usage()
            return false
        }
        else {
            config = (String)options.c
            connect = (String)options.s
        }
        
        if (options.l) logLevel = new Integer((String)options.l) 
    }

    public boolean authenticate() {
        System.out.println("Please log in")

        Scanner scan = new Scanner(System.in);

        int loginAttempts = 0;

        while (agent == null && loginAttempts++ < 3) {
            System.out.print("User:");
            String name = scan.nextLine();

            System.out.print("Password:");
            String pass = scan.nextLine();

            try {
                agent = Gateway.connect(name, pass, null);
            }
            catch (Exception ex) {
                System.err.println(ex.getMessage());
            }
        }
        scan.close();

        return agent != null
    }
}
