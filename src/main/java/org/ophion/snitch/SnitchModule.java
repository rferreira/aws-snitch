package org.ophion.snitch;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.google.inject.Binder;
import com.google.inject.Module;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalINIConfiguration;


/**
 * Created by IntelliJ IDEA.
 * User: raferrei
 * Date: Nov 6, 2011
 * Time: 9:51:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class SnitchModule implements Module {
    private String configFile;

    public SnitchModule(String configFile) {
        this.configFile = configFile;
    }

    @Override
    public void configure(Binder binder) {
        try {
            HierarchicalINIConfiguration config = new HierarchicalINIConfiguration(configFile);

            String awsAccess = config.getString("aws.access");
            String awsSecret = config.getString("aws.secret");

            if ( (awsAccess.length() == 0) || (awsSecret.length() == 0)) {
                binder.addError("config file did not include aws access and/or secret keys!");
            }

            AWSCredentials creds = new BasicAWSCredentials(config.getString("aws.access"), config.getString("aws.secret"));

            binder.bind(AWSCredentials.class).toInstance(creds);
            binder.bind(Configuration.class).toInstance(config);
            
        } catch (ConfigurationException e) {
            binder.addError("error parsing config file", e);
        }

    }
}
