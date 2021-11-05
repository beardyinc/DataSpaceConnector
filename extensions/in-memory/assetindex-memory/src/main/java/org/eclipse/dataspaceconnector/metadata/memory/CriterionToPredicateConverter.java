package org.eclipse.dataspaceconnector.metadata.memory;

import org.eclipse.dataspaceconnector.spi.asset.Criterion;
import org.eclipse.dataspaceconnector.spi.asset.CriterionConverter;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Converts a {@link Criterion}, which is essentially a select statement, into a {@code Predicate<Asset>}.
 * <p>
 * This is useful when dealing with in-memory collections of objects, here: {@link Asset} where Predicates can be applied
 * efficiently.
 * <p>
 * _Note: other {@link org.eclipse.dataspaceconnector.spi.asset.AssetIndex} implementations might have different converters!
 */
public class CriterionToPredicateConverter implements CriterionConverter<Predicate<Asset>> {
    @Override
    public Predicate<Asset> convert(Criterion criterion) {
        if ("=".equals(criterion.getOperator())) {
            return asset -> {
                Object property = property((String) criterion.getOperandLeft(), asset);
                if (property == null) {
                    return false; //property does not exist on asset
                }
                return Objects.equals(property, criterion.getOperandRight());
            };
        }
        throw new IllegalArgumentException(String.format("Operator [%s] is not supported by this converter!", criterion.getOperator()));
    }


    private Object property(String key, Asset asset) {
        if (asset.getProperties() == null || asset.getProperties().isEmpty()) {
            return null;
        }
        return asset.getProperty(key);
    }
}
