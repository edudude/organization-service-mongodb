package org.exoplatform.extension.organization.test;

import org.exoplatform.extension.organization.mongodb.MongoDBManager;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.picocontainer.Startable;

import de.flapdoodle.embed.mongo.Command;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.config.RuntimeConfigBuilder;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.config.io.ProcessOutput;
import de.flapdoodle.embed.process.io.ConsoleOutputStreamProcessor;
import de.flapdoodle.embed.process.io.IStreamProcessor;
import de.flapdoodle.embed.process.io.NullProcessor;

/**
 * 
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * 
 */
public class StandaloneMongoDBServer implements Startable {

  private final Logger log = LoggerFactory.getLogger(MongoDBManager.class);

  protected MongodExecutable mongodExe;

  protected MongodProcess mongod;

  public StandaloneMongoDBServer() {
    // To be sure that it starts in time
    start();
  }

  @Override
  public void start() {
    if (mongod != null) {
      return;
    }
    log.info("*** Start Mongo DB Standalone Server ***");
    try {
      IStreamProcessor mongodOutput = new NullProcessor();
      IStreamProcessor mongodError = new ConsoleOutputStreamProcessor();
      IStreamProcessor commandsOutput = new NullProcessor();
      IRuntimeConfig runtimeConfig = new RuntimeConfigBuilder().defaults(Command.MongoD).processOutput(new ProcessOutput(mongodOutput, mongodError, commandsOutput)).build();
      MongodStarter runtime = MongodStarter.getInstance(runtimeConfig);

      IMongodConfig mongodConfig = new MongodConfigBuilder().version(Version.V2_4_8).net(new Net(27777, false)).build();

      mongodExe = runtime.prepare(mongodConfig);
      mongod = mongodExe.start();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void stop() {
    if (mongodExe != null) {
      mongodExe.stop();
    }
  }
}
