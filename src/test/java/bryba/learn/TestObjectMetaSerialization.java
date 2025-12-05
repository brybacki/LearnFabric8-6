/*
 * Copyright Starburst Data, Inc. All rights reserved.
 *
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF STARBURST DATA.
 * The copyright notice above does not evidence any
 * actual or intended publication of such source code.
 *
 * Redistribution of this material is strictly prohibited.
 */
package bryba.learn;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.utils.KubernetesSerialization;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Test to demonstrate ObjectMeta serialization and deserialization behavior,
 * particularly how it handles null and missing values.
 */
public class TestObjectMetaSerialization
{
    @Test
    public void testSerializationOmitsNullAndEmptyFields()
            throws Exception
    {
        // Create ObjectMeta with only some fields set
        ObjectMeta metadata = new ObjectMetaBuilder()
                .withName("test-pod")
                .withNamespace("default")
                .build();

        // Serialize to JSON using Fabric8's serializer
        String json = new KubernetesSerialization().asJson(metadata);
        System.out.println("Serialized JSON:");
        System.out.println(json);

        // Verify that only non-null/non-empty fields are included
        assertNotNull(json);
        assert json.contains("\"name\":\"test-pod\"");
        assert json.contains("\"namespace\":\"default\"");
        // Empty collections and null fields should be omitted
        assert !json.contains("\"labels\"");
        assert !json.contains("\"annotations\"");
    }

    @Test
    public void testDeserializationWithMissingFields()
            throws Exception
    {
        // JSON with minimal fields
        String json = "{\"name\":\"test-pod\",\"namespace\":\"default\"}";

        // Deserialize using Fabric8's deserializer
        ObjectMeta metadata = new KubernetesSerialization().unmarshal(json, ObjectMeta.class);

        System.out.println("\nDeserialized ObjectMeta:");
        System.out.println("  name: " + metadata.getName());
        System.out.println("  namespace: " + metadata.getNamespace());
        System.out.println("  labels: " + metadata.getLabels());
        System.out.println("  annotations: " + metadata.getAnnotations());

        // Verify behavior: missing fields are set to null (not empty collections)
        assertEquals("test-pod", metadata.getName());
        assertEquals("default", metadata.getNamespace());
        assertNull(metadata.getLabels(), "Missing labels should be null, not empty map");
        assertNull(metadata.getAnnotations(), "Missing annotations should be null, not empty map");
    }

    @Test
    public void testDeserializationWithExplicitNullFields()
            throws Exception
    {
        // JSON with explicit null values
        String json = "{\"name\":\"test-pod\",\"namespace\":\"default\",\"labels\":null,\"annotations\":null}";

        ObjectMeta metadata = new KubernetesSerialization().unmarshal(json, ObjectMeta.class);

        System.out.println("\nDeserialized with explicit nulls:");
        System.out.println("  labels: " + metadata.getLabels());
        System.out.println("  annotations: " + metadata.getAnnotations());

        // Verify that explicit nulls are preserved as null
        assertNull(metadata.getLabels());
        assertNull(metadata.getAnnotations());
    }

    @Test
    public void testDeserializationWithEmptyCollections()
            throws Exception
    {
        // JSON with empty maps
        String json = "{\"name\":\"test-pod\",\"namespace\":\"default\",\"labels\":{},\"annotations\":{}}";

        ObjectMeta metadata = new KubernetesSerialization().unmarshal(json, ObjectMeta.class);

        System.out.println("\nDeserialized with empty maps:");
        System.out.println("  labels: " + metadata.getLabels());
        System.out.println("  annotations: " + metadata.getAnnotations());

        // Verify that empty collections are preserved as empty (not null)
        assertNotNull(metadata.getLabels());
        assertNotNull(metadata.getAnnotations());
        assertEquals(0, metadata.getLabels().size());
        assertEquals(0, metadata.getAnnotations().size());
    }

    @Test
    public void testRoundTripSerialization()
            throws Exception
    {
        // Create ObjectMeta with various fields
        Map<String, String> labels = new HashMap<>();
        labels.put("app", "my-app");
        labels.put("version", "1.0");

        ObjectMeta original = new ObjectMetaBuilder()
                .withName("test-pod")
                .withNamespace("default")
                .withLabels(labels)
                .build();

        // Serialize
        String json = Serialization.asJson(original);
        System.out.println("\nRound-trip JSON:");
        System.out.println(json);

        // Deserialize
        ObjectMeta deserialized = new KubernetesSerialization().unmarshal(json, ObjectMeta.class);

        // Verify round-trip preserves data
        assertEquals(original.getName(), deserialized.getName());
        assertEquals(original.getNamespace(), deserialized.getNamespace());
        assertEquals(original.getLabels(), deserialized.getLabels());
    }

    @Test
    public void testStandardJacksonMapperBehavior()
            throws Exception
    {
        // Compare with standard Jackson behavior (without Fabric8 configuration)
        ObjectMapper standardMapper = new ObjectMapper();

        String json = "{\"name\":\"test-pod\",\"namespace\":\"default\"}";
        ObjectMeta metadata = standardMapper.readValue(json, ObjectMeta.class);

        System.out.println("\nStandard Jackson behavior:");
        System.out.println("  labels: " + metadata.getLabels());
        System.out.println("  annotations: " + metadata.getAnnotations());

        // Standard Jackson also sets missing fields to null
        assertNull(metadata.getLabels());
        assertNull(metadata.getAnnotations());
    }
}
