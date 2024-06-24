package edu.umn.cs.spoton.front.generators;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.internal.GeometricDistribution;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


/**
 * Similar to NonTrackingGenerationStatus, except it allows various mean for the geometric distribution.
 */
public class FlexibleNonTrackingGenerationStatus implements GenerationStatus {
    private final int MEAN_SIZE = 65;

    private final SourceOfRandomness random;
    private final Map<Key<?>, Object> contextValues = new HashMap<>();
    private final GeometricDistribution geometric = new GeometricDistribution();


    public FlexibleNonTrackingGenerationStatus(SourceOfRandomness random) {
        this.random = random;
    }

    @Override
    public int size() {
        return geometric.sampleWithMean(MEAN_SIZE, random);
    }

    @Override
    public int attempts() {
        throw new UnsupportedOperationException("attempts() and @ValueOf" +
                " is not supported in guided mode.");
    }

    @Override
    public <T> GenerationStatus setValue(Key<T> key, T value) {
        contextValues.put(key, value);
        return this;
    }

    @Override
    public <T> Optional<T> valueOf(Key<T> key) {
        return Optional.ofNullable(key.cast(contextValues.get(key)));
    }
}
