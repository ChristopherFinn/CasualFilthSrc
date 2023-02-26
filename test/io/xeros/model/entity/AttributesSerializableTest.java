package io.xeros.model.entity;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.gson.reflect.TypeToken;
import io.xeros.model.AttributesSerializable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AttributesSerializableTest {

    private static final String DIRECTORY = "./temp/";
    private static final String FILE = DIRECTORY + "attributes_test.json";

    private void makeDirectories() {
        if (!new File(DIRECTORY).exists() && !new File(DIRECTORY).mkdirs()) {
            fail();
            throw new IllegalStateException();
        }

        if (new File(FILE).exists() && !new File(FILE).delete()) {
            fail();
            throw new IllegalStateException();
        }
    }

    @Test
    public void test_attributes_serializable() throws IOException {
        makeDirectories();

        List<String> stringList = Lists.newArrayList("Hello", "darkness", "my", "old", "friend");
        List<Double> numberList = Lists.newArrayList(1.0, 33.0, 47.0);

        AttributesSerializable attributesSerializable3 = new AttributesSerializable(FILE) {
            @Override
            public Type getType() {
                return new TypeToken<AttributesSerializable>(){}.getClass();
            }
        };

        AttributesSerializable attributesSerializable1 = AttributesSerializable.getFromFile(FILE, attributesSerializable3);
        attributesSerializable1.setString("test_attribute", "test_value");
        attributesSerializable1.setList("test_list", stringList);
        attributesSerializable1.setList("test_list2", numberList);


        AttributesSerializable attributesSerializable2 = AttributesSerializable.getFromFile(FILE, attributesSerializable1);
        assertEquals("test_value", attributesSerializable2.getString("test_attribute"));
        assertEquals(stringList, attributesSerializable2.getList("test_list"));
        assertEquals(numberList, attributesSerializable2.getList("test_list2"));
    }

}