package org.ophion.snitch.handlers;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.PublishRequest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.configuration.Configuration;
import org.ophion.snitch.util.IEventHandler;
import org.ophion.snitch.util.Log;

/**
 * Date: Nov 7, 2011
 * Time: 11:20:37 PM
 */

@Singleton
public class SNSEventHandler implements IEventHandler {
    private AWSCredentials creds;
    private Configuration config;
    private static final Log LOG = new Log();
    private String topic;
    private Gson gson;

    @Inject
    public SNSEventHandler(Configuration config, AWSCredentials creds) {
        this.creds = creds;
        this.config = config;
        this.topic = config.getString("sns.topic");

        if (topic == null) {
            throw new RuntimeException("could not find the desired sns topic in the config file, please add topic: arn:foo!");
        }

        LOG.fine("using topic: %s", topic);

        boolean formatJSON = config.getBoolean("sns.format_json", false);

        if (formatJSON) {
            gson = new GsonBuilder().setPrettyPrinting().create();
            LOG.info("using formatted json");
        } else {
            gson = new Gson();
        }

    }

    @Override
    public void handle(Object eventSource, Object eventArgs) {
        Instance instance = (Instance) eventArgs;
        LOG.fine("handling instance: %s ", instance.getInstanceId());

        try {
            AmazonSNS sns = new AmazonSNSClient(creds);
            sns.publish(new PublishRequest(topic, gson.toJson(instance), String.format("event instance %s (%s) state: %s", instance.getInstanceId(), instance.getPrivateDnsName(), instance.getState().getName())));
        } catch (Exception ex) {
            LOG.severe("error publishing event:", ex);
        }
    }
}
