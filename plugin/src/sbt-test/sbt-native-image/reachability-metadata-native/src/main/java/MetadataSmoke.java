import ch.qos.logback.core.Layout;
import ch.qos.logback.core.util.OptionHelper;

public class MetadataSmoke {
    public static void main(String[] args) throws Exception {
        OptionHelper.instantiateByClassName(
            args[0], Layout.class, MetadataSmoke.class.getClassLoader());
    }
}
