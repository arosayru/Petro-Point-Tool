
package petro.point.tool;
import java.sql.*;

public class DBConnection {
      private static Connection con;
  public static Connection getdbconnection()
  {
      try{
          String dbp="jdbc:mysql://localhost:3306/map";
          con=DriverManager.getConnection(dbp,"root","");
         }
      catch(SQLException e)
      {
          System.err.println(e.getMessage());
      }
      return con;
  }
}
