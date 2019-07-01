/*
 * This file is part of helper, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.helper.config.typeserializers;

import com.google.common.reflect.TypeToken;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;

import java.util.*;

public final class BukkitTypeSerializer implements TypeSerializer<ConfigurationSerializable> {
    public static final BukkitTypeSerializer INSTANCE = new BukkitTypeSerializer();

    private BukkitTypeSerializer() {
    }

    @Override
    public ConfigurationSerializable deserialize(TypeToken<?> type, ConfigurationNode from) throws ObjectMappingException {
        Map<String, Object> map = (Map<String, Object>) from.getValue();
        deserializeChildren(map);
        return ConfigurationSerialization.deserializeObject(map);
    }

    @Override
    public void serialize(TypeToken<?> type, ConfigurationSerializable from, ConfigurationNode to) throws ObjectMappingException {
        to.setValue(serializableToMap(from));
    }

    private List<Object> serializeChildren(List<Object> collection) {
        ListIterator<Object> iterator = collection.listIterator();
        while(iterator.hasNext()) {
            Object entry = iterator.next();
            if(entry instanceof Map) {
                try {
                    //noinspection unchecked
                    Map<String, Object> value = (Map<String, Object>) entry;
                    iterator.set(serializeChildren(value));
                } catch (Exception ignore) {
                }
            } else if(entry instanceof List) {
                try {
                    //noinspection unchecked
                    List<Object> value = (List<Object>) entry;
                    iterator.set(serializeChildren(value));
                } catch (Exception ignore) {
                }
            } else if(entry instanceof ConfigurationSerializable) {
                ConfigurationSerializable value = (ConfigurationSerializable) entry;
                iterator.set(serializeChildren(serializableToMap(value)));
            }
        }

        return collection;
    }

    private Map<String, Object> serializeChildren(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if(entry.getValue() instanceof Map) {
                try {
                    //noinspection unchecked
                    Map<String, Object> value = (Map<String, Object>) entry.getValue();
                    entry.setValue(serializeChildren(value));
                } catch (Exception ignore) {
                }
            } else if(entry.getValue() instanceof List) {
                try {
                    //noinspection unchecked
                    List<Object> value = (List<Object>) entry.getValue();
                    entry.setValue(serializeChildren(value));
                } catch (Exception ignore) {
                }
            } else if(entry.getValue() instanceof ConfigurationSerializable) {
                ConfigurationSerializable value = (ConfigurationSerializable) entry.getValue();
                entry.setValue(serializeChildren(serializableToMap(value)));
            }
        }

        return map;
    }

    private Map<String, Object> serializableToMap(ConfigurationSerializable from) {
        Map<String, Object> serialized = serializeChildren(from.serialize());

        Map<String, Object> map = new LinkedHashMap<>(serialized.size() + 1);
        map.put(ConfigurationSerialization.SERIALIZED_TYPE_KEY, ConfigurationSerialization.getAlias(from.getClass()));
        map.putAll(serialized);

        return map;
    }

    private static void deserializeChildren(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() instanceof Map) {
                try {
                    //noinspection unchecked
                    Map<String, Object> value = (Map) entry.getValue();

                    deserializeChildren(value);

                    if (value.containsKey("==")) {
                        entry.setValue(ConfigurationSerialization.deserializeObject(value));
                    }

                } catch (Exception e) {
                    // ignore
                }
            }

            if (entry.getValue() instanceof Number) {
                double doubleVal = ((Number) entry.getValue()).doubleValue();
                int intVal = (int) doubleVal;
                long longVal = (long) doubleVal;

                if (intVal == doubleVal) {
                    entry.setValue(intVal);
                } else if (longVal == doubleVal) {
                    entry.setValue(longVal);
                } else {
                    entry.setValue(doubleVal);
                }
            }
        }
    }
}
