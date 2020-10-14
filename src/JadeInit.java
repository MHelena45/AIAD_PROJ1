import jade.core.ProfileImpl;
import jade.wrapper.AgentContainer;
import jade.core.Profile;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ControllerException;
import jade.wrapper.StaleProxyException;

public class JadeInit {
    public static void main(String[] args) {
        //Start JADE
        Runtime runt = Runtime.instance();
        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.GUI, "true");

        //Add agents
        AgentContainer mainContainer = runt.createMainContainer(profile);
        try {
            AgentController agentController = mainContainer.createNewAgent("BadCode Inc.", "Store", null);
        } catch (StaleProxyException e) {
            System.err.println("\nThere was an error creating the agent!");
            e.printStackTrace();
        }
        System.out.println("Agent created...");

        try {
            mainContainer.start();
        } catch (ControllerException e) {
            System.err.println("\nThere was an error with the main Controller!");
            e.printStackTrace();
        }

        System.out.println("Container Running....");
    }
}
