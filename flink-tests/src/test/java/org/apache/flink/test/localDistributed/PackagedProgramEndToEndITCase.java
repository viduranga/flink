/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.test.localDistributed;

import java.io.File;
import java.io.FileWriter;

import org.apache.flink.client.RemoteExecutor;
import org.apache.flink.configuration.ConfigConstants;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.test.testdata.KMeansData;
import org.apache.flink.test.util.ForkableFlinkMiniCluster;
import org.junit.Assert;
import org.junit.Test;


public class PackagedProgramEndToEndITCase {
	
	private static final String JAR_PATH = "target/kmeans-test-jar.jar";

	@Test
	public void testEverything() {
		ForkableFlinkMiniCluster cluster = null;

		File points = null;
		File clusters = null;
		File outFile = null;
		
		try {
			// set up the files
			points = File.createTempFile("kmeans_points", ".in");
			clusters = File.createTempFile("kmeans_clusters", ".in");
			outFile = File.createTempFile("kmeans_result", ".out");
			
			outFile.delete();

			FileWriter fwPoints = new FileWriter(points);
			fwPoints.write(KMeansData.DATAPOINTS);
			fwPoints.close();

			FileWriter fwClusters = new FileWriter(clusters);
			fwClusters.write(KMeansData.INITIAL_CENTERS);
			fwClusters.close();

			

			// run KMeans
			Configuration config = new Configuration();
			config.setInteger(ConfigConstants.LOCAL_INSTANCE_MANAGER_NUMBER_TASK_MANAGER, 2);
			config.setInteger(ConfigConstants.TASK_MANAGER_NUM_TASK_SLOTS, 2);
			cluster = new ForkableFlinkMiniCluster(config, false);

			RemoteExecutor ex = new RemoteExecutor(ConfigConstants.JOB_MANAGER_IPC_ADDRESS_VALUE, cluster.getJobManagerRPCPort());

			ex.executeJar(JAR_PATH,
					"org.apache.flink.test.util.testjar.KMeansForTest",
					new String[] {
							points.toURI().toString(),
							clusters.toURI().toString(),
							outFile.toURI().toString(),
							"25"});

		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		finally {
			if (points != null) {
				points.delete();
			}
			if (cluster != null) {
				clusters.delete();
			}
			if (outFile != null) {
				outFile.delete();
			}
			
			try {
				if(cluster != null) {
					cluster.stop();
				}
			} catch (Exception e) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			}
		}
	}
}
