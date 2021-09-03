package org.cache2k.benchmark;

/*
 * #%L
 * Benchmarks: Eviction variants, benchmark harness
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

import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.cache2k.IntCache;
import org.cache2k.integration.CacheLoader;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Jens Wilke; created: 2013-12-08
 */
public class Cache2kFactory extends ProductCacheFactory {

  private AtomicInteger counter = new AtomicInteger();
  private boolean disableStatistics = true;
  private boolean strictEviction = false;

  @Override
  protected <K, V> BenchmarkCache<K, V> createSpecialized(final Class<K> _keyType, final Class<V> _valueType, final int _maxElements) {
    final Cache<K, V> c = createInternal(_keyType, _valueType, _maxElements, null);
    return returnCache(c, _maxElements);
  }

  @SuppressWarnings("unchecked")
  private <K, V> BenchmarkCache<K, V> returnCache(final Cache<K, V> _c, final int _maxElements) {
    if (_c instanceof IntCache) {
      final IntCache<V> ic = (IntCache<V>) _c;
      return (BenchmarkCache<K, V>) new IntBenchmarkCache<V>() {

        @Override
        public V getIfPresent(int key) {
          return ic.peek(key);
        }

        @Override
        public void put(int key, V value) {
          ic.put(key, value);
        }

        @Override
        public void remove(int key) {
          ic.remove(key);
        }

        @Override
        public void close() {
          ic.close();
        }

        @Override
        public String toString() {
          return ic.toString();
        }
      };
    }
    return new BenchmarkCache<K, V>() {

      @Override
      public V get(K key) {
        return _c.peek(key);
      }

      @Override
      public void put(K key, V value) {
        _c.put(key, value);
      }

      @Override
      public void remove(final K key) {
        _c.remove(key);
      }

      @Override
      public void close() {
        _c.close();
      }

      @Override
      public String toString() {
        return _c.toString();
      }
    };
  }

  @SuppressWarnings("unchecked")
  @Override
  public <K, V> BenchmarkCache<K, V> createLoadingCache(final Class<K> _keyType, final Class<V> _valueType, final int _maxElements, final BenchmarkCacheLoader<K, V> _source) {
    final Cache<K, V> c = createInternal(_keyType, _valueType, _maxElements, _source);
    if (c instanceof IntCache) {
      final IntCache<V> ic = (IntCache<V>) c;
      return (BenchmarkCache<K, V>) new IntBenchmarkCache<V>() {
        @Override
        public V getIfPresent(final int key) {
          return ic.get(key);
        }

        @Override
        public void put(final int key, final V value) {
          ic.put(key, value);
        }

        @Override
        public void remove(final int key) { ic.remove(key); }

        @Override
        public String toString() {
          return ic.toString();
        }

        @Override
        public void close() {
          ic.close();
        }
      };
    }
    if (_keyType == Integer.class) {
      final Cache<Integer, V> ic = (Cache<Integer, V>) c;
      return (BenchmarkCache<K, V>) new IntBenchmarkCache<V>() {
        @Override
        public V get(final Integer key) {
          return ic.get(key);
        }

        @Override
        public void put(final Integer key, final V value) {
          ic.put(key, value);
        }

        @Override
        public void remove(final Integer key) { ic.remove(key); }

        @Override
        public String toString() {
          return ic.toString();
        }

        @Override
        public void close() {
          c.close();
        }
      };
    }
    return new BenchmarkCache<K, V>() {
      @Override
      public V get(final K key) {
        return c.get(key);
      }

      @Override
      public void put(final K key, final V value) {
        c.put(key, value);
      }

      @Override
      public void remove(final K key) {
        c.remove(key);
      }

      @Override
      public String toString() {
        return c.toString();
      }

      @Override
      public void close() {
        c.close();
      }
    };
  }

  private <K,V> Cache<K, V> createInternal(final Class<K> _keyType, final Class<V> _valueType, final int _maxElements, final BenchmarkCacheLoader<K, V> _source) {
    Cache2kBuilder<K, V> b =
      Cache2kBuilder.of(_keyType, _valueType)
        .name("testCache-" + counter.incrementAndGet())
        .entryCapacity(_maxElements)
        .refreshAhead(false)
        .strictEviction(strictEviction);
    if (withExpiry) {
      b.expireAfterWrite(2 * 60, TimeUnit.SECONDS);
    } else {
      b.eternal(true);
    }
    if (disableStatistics) {
      b.disableStatistics(true).strictEviction(false).boostConcurrency(true);
    } else {
      b.strictEviction(true);
    }
    final AtomicInteger _evictCount = new AtomicInteger();
    if (_source != null) {
      b.loader(new CacheLoader<K, V>() {
        @Override
        public V load(final K key) throws Exception {
          return _source.load(key);
        }
      });
    }
    return b.build();
  }

  public void setDisableStatistics(final boolean _disableStatistics) {
    disableStatistics = _disableStatistics;
  }

  public void setStrictEviction(final boolean _strictEviction) {
    strictEviction = _strictEviction;
  }
}
