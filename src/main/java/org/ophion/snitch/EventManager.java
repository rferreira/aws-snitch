package org.ophion.snitch;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.joda.time.Minutes;
import org.ophion.snitch.util.AWSUtils;
import org.ophion.snitch.util.EventSink;
import org.ophion.snitch.util.Log;

import java.text.ParseException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


@Singleton
public class EventManager {
    private Configuration config;
    private static final Log LOG = new Log();
    private static final int DEFAULT_POLL_PERIOD = 5;
    private AWSCredentials creds;


    // events
    public EventSink onInstanceEvent = new EventSink();
    public EventSink onStart = new EventSink();

    @Inject
    public EventManager(Configuration config, AWSCredentials creds) {
        this.config = config;
        this.creds = creds;
        LOG.fine("ctor");
    }

    public void start() throws InterruptedException {

        int pollPeriod = config.getInt("server.poll_period");

        if (pollPeriod < 1) {
            pollPeriod = DEFAULT_POLL_PERIOD;
        }

        LOG.info("starting - poll period %d minute(s)", pollPeriod);

        final int PERIOD = pollPeriod;

        onStart.dispatch(this, null);

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                Date startDT = new Date();
                LOG.fine("start time: " + startDT);

                try {

                    long sm = System.currentTimeMillis();

                    AmazonEC2Client EC2 = new AmazonEC2Client(creds);

                    DescribeInstancesResult describeInstancesRequest = EC2.describeInstances();
                    List<Reservation> reservations = describeInstancesRequest.getReservations();
                    Set<Instance> instances = new HashSet<Instance>();

                    for (Reservation reservation : reservations) {
                        instances.addAll(reservation.getInstances());
                    }

                    boolean changed;
                    Date eventDate = null;

                    for (Instance instance : instances) {
                        changed = false;

                        LOG.fine("processing instance %s state: %s", instance.getInstanceId(), instance.getState());
                        //LOG.fine(instance.toString());


                        // first we see when the machine was booted
                        if (happenedSinceLastRun(instance.getLaunchTime(), startDT, PERIOD)) {
                            changed = true;
                            eventDate = instance.getLaunchTime();
                        }


                        if (instance.getStateReason() != null) {
                            // trying to parse the transition date
                            try {
                                eventDate = AWSUtils.getStateTransitionDate(instance.getStateTransitionReason());
                                LOG.fine("looks like instance %s changed state on %s", instance.getInstanceId(), eventDate);

                                if (happenedSinceLastRun(eventDate, startDT, PERIOD)) {
                                    changed = true;
                                } else {
                                    LOG.fine("state change happened too long ago, " + eventDate + " ignoring");
                                }

                            } catch (ParseException e) {
                                LOG.severe("could not parse transition timestamp: %s ", instance.getStateReason(), e);
                            }

                        }

                        if (changed) {
                            LOG.info("event instance %s (%s) state: %s on %s", instance.getInstanceId(), instance.getPrivateDnsName(), instance.getState().getName(), eventDate);
                            onInstanceEvent.dispatchAsync(this, instance);
                        }

                    }

                    LOG.info("processed %d instance(s) in %d msec", instances.size(), System.currentTimeMillis() - sm);


                } catch (AmazonServiceException ase) {
                    System.out.println("Caught Exception: " + ase.getMessage());
                    System.out.println("Response Status Code: " + ase.getStatusCode());
                    System.out.println("Error Code: " + ase.getErrorCode());
                    System.out.println("Request ID: " + ase.getRequestId());
                }
            }
        }, 0, pollPeriod, TimeUnit.MINUTES);

        Thread.currentThread().join();

    }

    private boolean happenedSinceLastRun(Date event, Date whenRunStarted, int runInMinutes) {
        return Math.abs(Minutes.minutesBetween(new DateTime(whenRunStarted), new DateTime(event)).getMinutes()) <= runInMinutes;
    }
}
