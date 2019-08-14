package org.cache2k.benchmark.eviction;

/*
 * #%L
 * Benchmarks: Implementation and eviction variants
 * %%
 * Copyright (C) 2013 - 2019 headissue GmbH, Munich
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

import org.cache2k.benchmark.BenchmarkCache;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple cache based on a hash map witch delegates to a
 * {@link EvictionPolicy} policy for eviction decisions. Not thread safe. This is
 * just for evaluating and experimentation with different eviction strategies.
 *
 * @author Jens Wilke
 */
@SuppressWarnings("FieldCanBeLocal")
public class EvaluationCache<E extends Entry<K,V>, K,V>
	extends BenchmarkCache<K,V> {

	private final EvictionPolicy<K, V, E> eviction;
	private final Map<K, E> content = new HashMap<>();
	private final boolean evictAfter = true;
	private long evictSpinCnt = 0;

	public EvaluationCache(final EvictionPolicy<K, V, E> eviction) {
		this.eviction = eviction;
	}

	@Override
	public void put(final K key, final V value) {
		if (!evictAfter && content.size() >= eviction.getCapacity()) {
			E e0 = eviction.evict();
			getEvictionListeners().forEach(l -> l.evicted(e0.getKey()));
			content.remove(e0.getKey());
		}
		E e = eviction.newEntry(key, value);
		if (evictAfter && content.size() >= eviction.getCapacity()) {
			for(;;) {
				E e0 = eviction.evict();
				getEvictionListeners().forEach(l -> l.evicted(e0.getKey()));
				if (e0.getKey().equals(key)) {
					e = eviction.newEntry(key, value);
					evictSpinCnt++;
					continue;
				}
				content.remove(e0.getKey());
				break;
			}
		}
		content.put(key, e);
	}

	@Override
	public V get(final K key) {
		E e = content.get(key);
		if (e != null) {
			eviction.recordHit(e);
			return e.getValue();
		}
		return null;
	}

	@Override
	public void remove(final K key) {
		E e = content.get(key);
		if (e != null) {
			eviction.remove(e);
		}
		content.remove(key);
	}

	@Override
	public int getCapacity() {
		return eviction.getCapacity();
	}

	@Override
	public void close() {
		eviction.close(content.size());
	}

	@Override
	public String toString() {
		return
		  "EvaluationCache(size=" + content.size() +
				", capacity=" + getCapacity() +
				( evictAfter ? ", evictAfter=true, evictSpinCnt=" + evictSpinCnt : "" ) +
				", eviction=" + eviction.toString() +
				")";
	}

}