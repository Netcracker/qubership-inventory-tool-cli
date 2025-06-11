/*
 * Copyright 2024-2025 NetCracker Technology Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.qubership.itool;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.google.inject.Module;

import org.qubership.itool.context.FlowContext;
import org.qubership.itool.di.ApplicationContext;
import org.qubership.itool.di.QubershipModule;
import org.qubership.itool.factories.JavaAppContextVerticleFactory;

import io.vertx.core.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.qubership.itool.modules.graph.Graph;

@ExtendWith(VertxExtension.class)
public class TestApplicationContextService {

  @Test
  public void testDependencyInjection() throws Throwable {
    VertxTestContext testContext = new VertxTestContext();
    Vertx vertx = Vertx.vertx();
    
    JsonObject config = new JsonObject();
    ApplicationContext appContext = new ApplicationContext(vertx, config, new Module[] {new QubershipModule(vertx)});

    FlowContext flowContext = appContext.getInstance(FlowContext.class);
    flowContext.initialize(vertx, config);

    vertx.registerVerticleFactory(new JavaAppContextVerticleFactory(flowContext, config));
    DeploymentOptions options = new DeploymentOptions().setWorker(true);

    TestResourceVerticle verticle = new TestResourceVerticle();
    flowContext.initialize(verticle);
    vertx.deployVerticle(verticle, options, deployHandler -> {
      if (deployHandler.succeeded()) {
        System.out.println("TRY SEND message to the test address (1)");
        vertx.setTimer(2000, id -> {
          System.out.println("TRY SEND message to the test address (2)");
          vertx.eventBus().request("test", null, replyHandler -> {
            if (replyHandler.succeeded()) {
              Object response = replyHandler.result().body();
              System.out.println("!!!!!! Received reply: " + response);
              if (response instanceof String && ((String) response).length() > 20) {
                testContext.completeNow();
              } else {
                testContext.failNow(new IllegalStateException("" + response));
              }
            } else {
              testContext.failNow(replyHandler.cause());
            }
          });
        });
      } else {
        testContext.failNow(deployHandler.cause());
      }
    });

    assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
        throw testContext.causeOfFailure();
    }

    vertx.close();
  }

  public class TestResourceVerticle extends AbstractVerticle {

    @Resource
    private Graph graph;

    @Override
    public void start() throws Exception {
      System.out.println("TestResourceVerticle start");
      Vertx vertx = getVertx();
      EventBus eventBus = vertx.eventBus();
      MessageConsumer<Object> consumer = eventBus.consumer("test", message -> {
        System.out.println("Test received message");
        message.reply("" + graph);
      });
      consumer.completionHandler(handler -> {
        if (handler.succeeded()) {
          System.out.println("Test verticle registered on the bus");
        }
      });
    }

  }

}
