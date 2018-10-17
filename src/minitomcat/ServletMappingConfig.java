package minitomcat;

import java.util.ArrayList;
import java.util.List;

public class ServletMappingConfig {

    public static List<ServletMapping> servletMappingList = new ArrayList<>();

    static {
        servletMappingList.add(new ServletMapping("girl", "/girl", "minitomcat.MyGirlServlet"));
        servletMappingList.add(new ServletMapping("helloworld", "/helloworld", "minitomcat.MyHelloWorldServlet"));


    }

}
