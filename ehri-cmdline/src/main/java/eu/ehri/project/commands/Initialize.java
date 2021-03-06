package eu.ehri.project.commands;

import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.utils.GraphInitializer;
import org.apache.commons.cli.CommandLine;

/**
 * Import EAD from the command line...
 * 
 */
public class Initialize extends BaseCommand implements Command {
    
    final static String NAME = "initialize";


    public Initialize() {
    }

    @Override
    protected void setCustomOptions() {
    }

    @Override
    public String getHelp() {
        return "Usage: initialize";
    }

    @Override
    public String getUsage() {
        String help = "Initialize graph DB with minimal nodes (admin account, permissions, types).";
        return help;
    }

    /**
     * Command-line entry-point (for testing.)
     * 
     * @param graph
     * @param cmdLine
     * @throws Exception 
     */
    @Override
    public int execWithOptions(final FramedGraph<? extends TransactionalGraph> graph, CommandLine cmdLine) throws Exception {
        GraphInitializer initializer = new GraphInitializer(graph);
        initializer.initialize();
        return 0;
    }
}
