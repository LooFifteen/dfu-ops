package dev.lu15.dfu.ops.bson;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.bson.BsonArray;
import org.bson.BsonBoolean;
import org.bson.BsonDecimal128;
import org.bson.BsonDocument;
import org.bson.BsonDouble;
import org.bson.BsonInt32;
import org.bson.BsonInt64;
import org.bson.BsonNull;
import org.bson.BsonNumber;
import org.bson.BsonString;
import org.bson.BsonType;
import org.bson.BsonValue;
import org.bson.types.Decimal128;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class BsonOps implements DynamicOps<BsonValue> {

    public static final @NotNull BsonOps INSTANCE = new BsonOps();

    private BsonOps() {}

    @Override
    public BsonValue empty() {
        return new BsonDocument();
    }

    @Override
    public <U> U convertTo(DynamicOps<U> outOps, BsonValue input) {
        if (input instanceof BsonNull) return null;
        if (input instanceof BsonDocument document) return this.convertMap(outOps, document);
        if (input instanceof BsonArray array) return this.convertList(outOps, array);
        if (input instanceof BsonString string) return outOps.createString(string.getValue());
        if (input instanceof BsonBoolean booleanValue) return outOps.createBoolean(booleanValue.getValue());
        if (input instanceof BsonInt32 int32) return outOps.createNumeric(int32.intValue());
        if (input instanceof BsonInt64 int64) return outOps.createNumeric(int64.longValue());
        if (input instanceof BsonDouble doubleValue) return outOps.createNumeric(doubleValue.doubleValue());
        if (input instanceof BsonDecimal128 decimal128) return outOps.createNumeric(decimal128.decimal128Value());
        throw new IllegalArgumentException("don't know how to convert " + input);
    }

    @Override
    public DataResult<Number> getNumberValue(BsonValue input) {
        if (input instanceof BsonNumber number) {
            BsonType type = number.getBsonType();
            if (type == BsonType.INT32) return DataResult.success(number.intValue());
            if (type == BsonType.INT64) return DataResult.success(number.longValue());
            if (type == BsonType.DOUBLE) return DataResult.success(number.doubleValue());
            if (type == BsonType.DECIMAL128) return DataResult.success(number.decimal128Value());
        }
        return DataResult.error(() -> "not a number: " + input);
    }

    @Override
    public BsonValue createNumeric(Number i) {
        if (i instanceof Integer integer) return new BsonInt32(integer);
        if (i instanceof Short shortNumber) return new BsonInt32(shortNumber);
        if (i instanceof Long longNumber) return new BsonInt64(longNumber);
        if (i instanceof Double doubleNumber) return new BsonDouble(doubleNumber);
        if (i instanceof Float floatNumber) return new BsonDouble(floatNumber);
        if (i instanceof Decimal128 decimal128) return new BsonDecimal128(decimal128);
        throw new IllegalArgumentException("can't serialize number " + i);
    }

    @Override
    public DataResult<String> getStringValue(BsonValue input) {
        if (input instanceof BsonString string) return DataResult.success(string.getValue());
        return DataResult.error(() -> "not a string: " + input);
    }

    @Override
    public BsonValue createString(String value) {
        return new BsonString(value);
    }

    @Override
    public DataResult<Boolean> getBooleanValue(BsonValue input) {
        if (input instanceof BsonBoolean booleanValue) return DataResult.success(booleanValue.getValue());
        return DataResult.error(() -> "not a boolean: " + input);
    }

    @Override
    public BsonValue createBoolean(boolean value) {
        return new BsonBoolean(value);
    }

    @Override
    public DataResult<BsonValue> mergeToList(BsonValue list, BsonValue value) {
        if (!(list instanceof BsonArray array)) return DataResult.error(() -> "expected a list, got " + list, list);

        BsonArray result = new BsonArray();
        result.addAll(array);
        result.add(value);

        return DataResult.success(result);
    }

    @Override
    public DataResult<BsonValue> mergeToList(BsonValue list, List<BsonValue> values) {
        if (!(list instanceof BsonArray array)) return DataResult.error(() -> "expected a list, got " + list, list);

        BsonArray result = new BsonArray();
        result.addAll(array);
        result.addAll(values);

        return DataResult.success(result);
    }

    @Override
    public DataResult<BsonValue> mergeToMap(BsonValue map, BsonValue key, BsonValue value) {
        if (!(map instanceof BsonDocument document)) return DataResult.error(() -> "expected a map, got " + map, map);
        if (!(key instanceof BsonString string)) return DataResult.error(() -> "expected a string key, got " + key, map);

        BsonDocument result = new BsonDocument();
        result.putAll(document);
        result.put(string.getValue(), value);

        return DataResult.success(result);
    }

    @Override
    public DataResult<BsonValue> mergeToMap(BsonValue map, MapLike<BsonValue> values) {
        if (!(map instanceof BsonDocument document)) return DataResult.error(() -> "expected a map, got " + map, map);

        BsonDocument result = new BsonDocument();
        result.putAll(document);

        List<BsonValue> missed = Lists.newArrayList();
        values.entries().forEach(entry -> {
            BsonValue key = entry.getFirst();
            if (!(key instanceof BsonString string)) {
                missed.add(key);
                return;
            }
            result.put(string.getValue(), entry.getSecond());
        });

        if (!missed.isEmpty()) return DataResult.error(() -> "some keys are not strings: " + missed, result);

        return DataResult.success(result);
    }

    @Override
    public DataResult<Stream<Pair<BsonValue, BsonValue>>> getMapValues(BsonValue input) {
        if (!(input instanceof BsonDocument document)) return DataResult.error(() -> "not a map: " + input);

        return DataResult.success(
                document.entrySet().stream()
                        .map(entry -> Pair.of(
                                new BsonString(entry.getKey()),
                                entry.getValue() instanceof BsonValue value ? value : new BsonNull()
                        ))
        );
    }

    @Override
    public DataResult<Consumer<BiConsumer<BsonValue, BsonValue>>> getMapEntries(BsonValue input) {
        if (!(input instanceof BsonDocument document)) return DataResult.error(() -> "not a map: " + input);

        return DataResult.success(result -> {
            for (Map.Entry<String, BsonValue> entry : document.entrySet()) {
                result.accept(this.createString(entry.getKey()), entry.getValue() instanceof BsonNull ? this.empty() : entry.getValue());
            }
        });
    }

    @Override
    public DataResult<MapLike<BsonValue>> getMap(BsonValue input) {
        if (!(input instanceof BsonDocument document)) return DataResult.error(() -> "not a map: " + input);

        return DataResult.success(new MapLike<>() {
            @Nullable
            @Override
            public BsonValue get(BsonValue key) {
                BsonValue value = document.get(key.asString().getValue());
                if (value instanceof BsonNull) return null;
                return value;
            }

            @Nullable
            @Override
            public BsonValue get(String key) {
                BsonValue value = document.get(key);
                if (value instanceof BsonNull) return null;
                return value;
            }

            @Override
            public Stream<Pair<BsonValue, BsonValue>> entries() {
                return document.entrySet().stream().map(entry -> Pair.of(new BsonString(entry.getKey()), entry.getValue()));
            }

            @Override
            public String toString() {
                return "MapLike[" + document + "]";
            }
        });
    }

    @Override
    public BsonValue createMap(Stream<Pair<BsonValue, BsonValue>> map) {
        BsonDocument result = new BsonDocument();
        map.forEach(pair -> result.put(pair.getFirst().asString().getValue(), pair.getSecond()));
        return result;
    }

    @Override
    public DataResult<Stream<BsonValue>> getStream(BsonValue input) {
        if (!(input instanceof BsonArray array)) return DataResult.error(() -> "not a list: " + input);
        return DataResult.success(array.stream().map(value -> value instanceof BsonNull ? null : value));
    }

    @Override
    public DataResult<Consumer<Consumer<BsonValue>>> getList(BsonValue input) {
        if (!(input instanceof BsonArray array)) return DataResult.error(() -> "not a list: " + input);
        return DataResult.success(result -> array.forEach(value -> result.accept(value instanceof BsonNull ? null : value)));
    }

    @Override
    public BsonValue createList(Stream<BsonValue> input) {
        BsonArray result = new BsonArray();
        input.forEach(result::add);
        return result;
    }

    @Override
    public BsonValue remove(BsonValue input, String key) {
        if (input instanceof BsonDocument document) {
            BsonDocument result = new BsonDocument();
            document.entrySet().stream()
                    .filter(entry -> !Objects.equals(entry.getKey(), key))
                    .forEach(entry -> result.put(entry.getKey(), entry.getValue()));
            return result;
        }
        return input;
    }

}
