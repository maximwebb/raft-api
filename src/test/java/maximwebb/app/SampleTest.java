package maximwebb.app;

import org.junit.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SampleTest {
    @Test
    public void adderShouldAddCorrectly() {
        // EXECUTE
        int ans1 = Adder.add(3, 4);
        int ans2 = Adder.add(-4, 20);
        int ans3 = Adder.add(0, 0);

        // ASSERT
        assertEquals(ans1, 7);
        assertEquals(ans2, 16);
        assertEquals(ans3, 0);
    }
}
