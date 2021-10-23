package org.cache2k.benchmark.thirdparty;

/*
 * #%L
 * Benchmarks: third party products.
 * %%
 * Copyright (C) 2013 - 2021 headissue GmbH, Munich
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import org.cache2k.benchmark.EvictionBenchmarkRunnerRule;
import org.cache2k.benchmark.EvictionTestVariation;
import org.cache2k.benchmark.PrototypeCacheFactory;
import org.cache2k.benchmark.cache.Cache2kStarFactory;
import org.cache2k.benchmark.cache.CaffeineStarFactory;
import org.cache2k.benchmark.cache.EhCache3Factory;
import org.cache2k.benchmark.prototype.evictionPolicies.LruEviction;
import org.cache2k.benchmark.traces.Traces;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Compare the current cache2k eviction algorithm against the previous
 * one and Caffeine.
 *
 * @author Jens Wilke
 */
@RunWith(Parameterized.class)
public class EvictionComparisonBenchmark {

	public static final EvictionTestVariation.Builder CACHES = new EvictionTestVariation.Builder()
		// .add(PrototypeCacheFactory.of(Cache2kV14Eviction.class))
		// .add(PrototypeCacheFactory.of(Cache2kV12Eviction.class))
		.add(PrototypeCacheFactory.of(LruEviction.class))
		.add(new CaffeineStarFactory())
		.add(new Cache2kStarFactory())
		// TODO: EhCache3 is extremely slow, construct JMH benchmark
		// .add(new EhCache3Factory())
		;

	@ClassRule
	public static EvictionBenchmarkRunnerRule runner = new EvictionBenchmarkRunnerRule()
		.candidateAndPeers(CACHES)
		.setReadStoredResults(false);

	public static final EvictionTestVariation.Builder TRACES =
		new EvictionTestVariation.Builder()
			// .add(Traces.GLIMPSE)
			// .add(Traces.CORDA_LOOP_CORDA)
			// .add(Traces.GLIMPSE) // too small for meaningful comparisons
			// .add(Traces.CORDA_SMALL, 124) // very bad for cache2k, investigate
			.add(Traces.WEB12)
			.add(Traces.FINANCIAL1_1M)
			.add(Traces.OLTP, 128, 256, 512)
			.add(Traces.SCARAB_RECS)
			.add(Traces.LOOP)
		  .add(Traces.ZIPFIAN_10K_1M)
	;

	@Parameterized.Parameters(name="{0}")
	public static Iterable<? extends Object> data() {
		return new EvictionTestVariation.Builder()
			.merge(CACHES).merge(TRACES).build();
	}

	private final EvictionTestVariation variation;

	public EvictionComparisonBenchmark(EvictionTestVariation variation) {
		this.variation = variation;
	}

	@Test
	public void test() {
		runner.runBenchmark(variation);
	}

}