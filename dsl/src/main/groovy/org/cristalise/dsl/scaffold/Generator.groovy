package org.cristalise.dsl.scaffold

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

@CompileStatic
class Generator {

    public void main(String[] args) {
        readArgs(args)
    }

    @CompileDynamic
    private boolean readArgs(String[] args) {
        def cli = new CliBuilder(usage: 'AuthImporterCLI -[hcsorlad] authFile.csv')
        cli.width = 100

        cli.with {
            h longOpt: 'help', 'Show usage information'
            c longOpt: 'config', args: 1, argName: 'config.conf', 'Cristal-ise config file'
            s longOpt: 'connect', args: 1, argName: 'server.clc', 'Cristal-ise config file'
            o longOpt: 'outputFile', args: 1, argName: 'outputFile.log', 'Path where to store the output log'
            r longOpt: 'roles', args: 1, argName: 'roles', 'Comma separated list of roles to import'
            l longOpt: 'logLevel', args: 1, argName: 'logLevel', 'Set cristal-ise log level [0-9] '
            a longOpt: 'applyChanges', 'Apply the changes into the database'
            x longOpt: 'updateXMLs', 'Update bootstrap XML files'
            d longOpt: 'debug', 'Print extra information'
        }

        def options = cli.parse(args)

        // Show usage text when error or -h or --help option is used.
        if (!args || !options || options.h) {
            cli.usage(); return false
        }

        if (!options.arguments()) {
            println "Please provide input csv file"
            cli.usage()
            return false
        }

        if (!options.c || !options.s) {
            println "Please provide --config and --connect files"
            cli.usage()
            return false
        }

    }
}
