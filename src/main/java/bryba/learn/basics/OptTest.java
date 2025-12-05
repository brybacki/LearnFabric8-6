package bryba.learn.basics;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class OptTest
{
    public static void main(String[] args)
    {
        int a = Instant.now().atZone(ZoneOffset.UTC).getMinute() % 2;

        System.out.println(a);

        Optional<Long> o = Optional.of(-2L);

        Boolean result = o.map(val -> val >= 0)
                .orElse(true);
        System.out.println(result);

        List<String> ss = List.of("aa", "bb", "bbb", "cc");
        List<Integer> ll = ss.stream()
                .map(String::length)
                .map(l -> l > 2 ? null : l)
                .collect(Collectors.toList());
        System.out.println(ll);
    }
}
