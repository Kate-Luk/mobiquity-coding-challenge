import com.mobiquity.exception.APIException;
import com.mobiquity.packer.Packer;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PackerTest {
    
    @Test
    public void shouldReturnSeparateLineForEachValidInputString() throws APIException {
        String result = Packer.pack(Paths.get("src", "test", "resources", "valid_test_cases").toAbsolutePath().toString());
        String[] lines = result.split("\n");
        assertEquals(4, lines.length);
    }

    @Test
    public void shouldReturnIdsOfItemsWhichFitInThePack_testCase1() throws APIException {
        String result = Packer.pack(Paths.get("src", "test", "resources", "valid_test_cases").toAbsolutePath().toString());
        String[] lines = result.split("\n");

        String resultLine = lines[0];
        assertTrue(result.contains("2"));
        assertTrue(result.contains("7"));

        String[] idsWhichFitsInThePack = resultLine.split(",");
        assertEquals(2, idsWhichFitsInThePack.length);
    }

    @Test
    public void shouldReturnIdsOfItemsWhichFitInThePack_testCase2() throws APIException {
        String result = Packer.pack(Paths.get("src", "test", "resources", "valid_test_cases").toAbsolutePath().toString());
        String[] lines = result.split("\n");

        String resultLine = lines[2];
        assertTrue(result.contains("4"));

        String[] idsWhichFitsInThePack = resultLine.split(",");
        assertEquals(1, idsWhichFitsInThePack.length);
    }

    @Test
    public void shouldReturnTheSmallestWeightWhenMoreThanOneOptionWithTheSamePrice() throws APIException {
        String result = Packer.pack(Paths.get("src", "test", "resources", "more_than_one_package_with_the_same_price").toAbsolutePath().toString());
        String[] lines = result.split("\n");

        String resultLine = lines[0];
        assertTrue(result.contains("4"));
        assertTrue(result.contains("5"));
        assertTrue(result.contains("6"));

        String[] idsWhichFitsInThePack = resultLine.split(",");
        assertEquals(3, idsWhichFitsInThePack.length);
    }


    @Test
    public void shouldReturnEmptyStringWhenNoItemsFitInThePack() throws APIException {
        String actualResult = Packer.pack(Paths.get("src", "test", "resources", "no_items_fit_in_the_pack").toAbsolutePath().toString());
        assertEquals("-", actualResult);
    }

    @Test
    public void shouldThrowApiExceptionWhenFileIsEmpty() {
        APIException apiException = assertThrows(APIException.class, () -> {
            Packer.pack(Paths.get("src", "test", "resources", "empty_file").toAbsolutePath().toString());
        });
        assertEquals("The file is empty.", apiException.getMessage());
    }

    @Test
    public void shouldThrowApiExceptionWhenNoPackSize() {
        APIException apiException = assertThrows(APIException.class, () -> {
            Packer.pack(Paths.get("src", "test", "resources", "no_pack_size").toAbsolutePath().toString());
        });
        assertEquals("Incorrect parameters: could not parse a pack size in the line 0", apiException.getMessage());
    }

    @Test
    public void shouldThrowApiExceptionWhenNoColonDelimiter() {
        APIException apiException = assertThrows(APIException.class, () -> {
            Packer.pack(Paths.get("src", "test", "resources", "no_colon").toAbsolutePath().toString());
        });
        assertEquals("Incorrect parameters: there is no colon in the line 0", apiException.getMessage());
    }

    @Test
    public void shouldThrowApiExceptionWhenNoItems() {
        APIException apiException = assertThrows(APIException.class, () -> {
            Packer.pack(Paths.get("src", "test", "resources", "no_items").toAbsolutePath().toString());
        });
        assertEquals("Incorrect parameters: there is no items to pack in the line 0", apiException.getMessage());
    }

    @Test
    public void shouldThrowApiExceptionWhenFileContainsInvalidItems() {
        APIException apiException = assertThrows(APIException.class, () -> {
            Packer.pack(Paths.get("src", "test", "resources", "invalid_items_to_pack").toAbsolutePath().toString());
        });
        assertEquals("Incorrect parameters in the line 0: Item [1] has the wrong format.", apiException.getMessage());
    }


}
