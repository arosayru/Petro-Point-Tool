
package petro.point.tool;
import java.sql.*;
import java.text.SimpleDateFormat;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.swing.*;
import java.util.*;

public class FuelPumpInterface extends javax.swing.JFrame {
    
     private PetroPointInterface petroPointInterface;

     
    // Node for Singly Linked List
    class TransactionNode {
        
        String fuelType;
        double amount;
        String dateTime;
        TransactionNode next;

        public TransactionNode(String fuelType, int amount, String dateTime) {
            this.fuelType = fuelType;
            this.amount = amount;
            this.dateTime = dateTime;
            this.next = null;
        }
    }

    // Singly Linked List for Transactions
    class FuelTransactionList {
        TransactionNode head;
        TransactionNode tail;
        

        // Add a transaction to the linked list
        public void addTransaction(String fuelType, int amount) {
    String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    TransactionNode newNode = new TransactionNode(fuelType, amount, dateTime);

    if (head == null) {
        head = tail = newNode;
    } else {
        tail.next = newNode;
        tail = newNode;
    }

    // Perform database operations (insert and update) in a transactional way
    performDatabaseOperations(fuelType, amount, dateTime);
    calculatePumpStats(fuelType); 
    }

        // Display all transactions
        public void displayTransactions() {
            TransactionNode current = head;
            while (current != null) {
                System.out.println("Fuel Type: " + current.fuelType + ", Amount: " + current.amount + ", Date-Time: " + current.dateTime);
                current = current.next;
            }
        }

        // Save transaction to database
       private void performDatabaseOperations(String fuelType, int amount, String dateTime) {
        String pumpTable;
            String stockTable;

   if (fuelType.equals("Petrol")) {
                pumpTable = "petrolpump";
                stockTable = "petrolstock";
            } else {
                pumpTable = "dieselpump";
                stockTable = "dieselstock";
            }

            try (Connection con = DBConnection.getdbconnection()) {
                // Start a transaction
                con.setAutoCommit(false);

                // Insert into pump table
                try (PreparedStatement insertStmt = con.prepareStatement(
                        "INSERT INTO " + pumpTable + " (amount, datetime) VALUES (?, ?)")) {
                    insertStmt.setInt(1, amount);
                    insertStmt.setString(2, dateTime);
                    insertStmt.executeUpdate();
                }

        // Deduct stock row by row
        deductStock(con, stockTable, amount);

        // Commit the transaction if both operations succeed
        con.commit();
        System.out.println("Transaction successfully inserted and stock updated.");
    } catch (Exception e) {
        e.printStackTrace();
        try {
            // Roll back if there is an issue
            //con.rollback();
            JOptionPane.showMessageDialog(null, "Error occurred, transaction rolled back: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception rollbackEx) {
            rollbackEx.printStackTrace();
        }
    }
}

// Deduct stock row by row
private void deductStock(Connection con, String stockTable, int amountToDeduct) throws Exception {
    // Query to get rows without any specific ordering
    String fetchStockQuery;
            String idColumn;
            
if (stockTable.equals("petrolstock")) {
                fetchStockQuery = "SELECT psid, amount FROM " + stockTable + " ORDER BY psid DESC";
                idColumn = "psid";
            } else {
                fetchStockQuery = "SELECT dsid, amount FROM " + stockTable + " ORDER BY dsid DESC";
                idColumn = "dsid";
            }

   try (PreparedStatement fetchStmt = con.prepareStatement(fetchStockQuery);
                 ResultSet rs = fetchStmt.executeQuery()) {

                while (rs.next() && amountToDeduct > 0) {
                    int id = rs.getInt(idColumn);
                    int currentAmount = rs.getInt("amount");

                    // Determine the deduction amount for this row
                    int deduction = Math.min(amountToDeduct, currentAmount);

                    // Update the current row's stock
                    try (PreparedStatement updateStmt = con.prepareStatement(
                            "UPDATE " + stockTable + " SET amount = amount - ? WHERE " + idColumn + " = ?")) {
                        updateStmt.setInt(1, deduction);
                        updateStmt.setInt(2, id);
                        updateStmt.executeUpdate();
                    }

            // Reduce the remaining amount to deduct
            amountToDeduct -= deduction;
        }

        if (amountToDeduct > 0) {
            throw new Exception("Not enough stock available to fulfill the request.");
        }
    }
}

    }

    private FuelTransactionList transactionList = new FuelTransactionList();

    public FuelPumpInterface() {
        initComponents();
        calculatePumpStats("Petrol");
        
        //******** Fetch the fuel prices*************************
        Map<String, String> fuelPrices = FetchFuelPrices.fetchFuelPrices();

        // Set the prices in the text fields
        Pprice_txt.setText(fuelPrices.getOrDefault("Petrol", "Price not available"));
        Dprice_txt.setText(fuelPrices.getOrDefault("Diesel", "Price not available"));


    petroPointInterface = new PetroPointInterface();
        
        petroPointInterface = new PetroPointInterface();
    }
    
    private void calculatePumpStats(String fuelType) {
        String pumpTable = fuelType.equals("Petrol") ? "petrolpump" : "dieselpump";
        ArrayList<Integer> pumpAmounts = new ArrayList<>();

        // Step 1: Fetch Data from Database
        try (Connection con = DBConnection.getdbconnection();
             PreparedStatement stmt = con.prepareStatement("SELECT amount FROM " + pumpTable);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                pumpAmounts.add(rs.getInt("amount"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error fetching pump data: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (pumpAmounts.isEmpty()) {
            minpump_txt.setText("0");
            maxpump_txt.setText("0");
            avaragepump_txt.setText("0.00");
            return;
        }

        // Step 2: Sort the Data
        quickSort(pumpAmounts, 0, pumpAmounts.size() - 1);

        // Step 3: Calculate Min, Max, and Average
        int min = pumpAmounts.get(0);
        int max = pumpAmounts.get(pumpAmounts.size() - 1);
        double average = pumpAmounts.stream().mapToDouble(Integer::doubleValue).average().orElse(0);

        // Step 4: Display in JTextFields
        minpump_txt.setText(String.valueOf(min+"L"));
        maxpump_txt.setText(String.valueOf(max+"L"));
        avaragepump_txt.setText(String.format("%.2fL", average));
    }

    // QuickSort Implementation
    private void quickSort(ArrayList<Integer> list, int low, int high) {
        if (low < high) {
            int pivotIndex = partition(list, low, high);
            quickSort(list, low, pivotIndex - 1);
            quickSort(list, pivotIndex + 1, high);
        }
    }

    private int partition(ArrayList<Integer> list, int low, int high) {
        int pivot = list.get(high);
        int i = low - 1;
        for (int j = low; j < high; j++) {
            if (list.get(j) <= pivot) {
                i++;
                Collections.swap(list, i, j);
            }
        }
        Collections.swap(list, i + 1, high);
        return i + 1;
    }
    
    
    
 


    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        Pprice_txt = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        Dprice_txt = new javax.swing.JTextField();
        FuelType_Combo_for_calculate = new javax.swing.JComboBox<>();
        pump_text = new javax.swing.JTextField();
        btn_pump = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        btn_back = new javax.swing.JButton();
        btn_clr = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JSeparator();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        maxpump_txt = new javax.swing.JTextField();
        minpump_txt = new javax.swing.JTextField();
        avaragepump_txt = new javax.swing.JTextField();
        pump_valuse_of_date = new javax.swing.JTextField();
        btn_get_pump_valuse_by_date = new javax.swing.JButton();
        jDateChooser_from_date = new com.toedter.calendar.JDateChooser();
        money_amount_of_fuel = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        jSeparator2 = new javax.swing.JSeparator();
        jDateChooser_to_date = new com.toedter.calendar.JDateChooser();
        jLabel10 = new javax.swing.JLabel();
        jPanel6 = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel1.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel2.setBackground(new java.awt.Color(178, 0, 0));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setText("PETRO POINT");
        jPanel2.add(jLabel1);

        jPanel1.add(jPanel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 660, 40));

        jPanel3.setBackground(new java.awt.Color(178, 0, 0));
        jPanel1.add(jPanel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 480, 660, 50));

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel3.setText("Today petrol Price : ");
        jPanel1.add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 50, 140, 30));

        Pprice_txt.setEditable(false);
        Pprice_txt.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        Pprice_txt.setForeground(new java.awt.Color(255, 0, 0));
        jPanel1.add(Pprice_txt, new org.netbeans.lib.awtextra.AbsoluteConstraints(180, 50, 120, 30));

        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel2.setText("Today diesel Price :");
        jPanel1.add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(370, 50, 140, 30));

        Dprice_txt.setEditable(false);
        Dprice_txt.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        Dprice_txt.setForeground(new java.awt.Color(255, 0, 0));
        jPanel1.add(Dprice_txt, new org.netbeans.lib.awtextra.AbsoluteConstraints(510, 50, 120, 30));

        FuelType_Combo_for_calculate.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        FuelType_Combo_for_calculate.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Petrol", "Diesel" }));
        FuelType_Combo_for_calculate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                FuelType_Combo_for_calculateActionPerformed(evt);
            }
        });
        jPanel1.add(FuelType_Combo_for_calculate, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 140, 210, 30));

        pump_text.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        pump_text.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pump_textActionPerformed(evt);
            }
        });
        jPanel1.add(pump_text, new org.netbeans.lib.awtextra.AbsoluteConstraints(480, 310, 150, 40));

        btn_pump.setBackground(new java.awt.Color(178, 0, 0));
        btn_pump.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btn_pump.setForeground(new java.awt.Color(255, 255, 255));
        btn_pump.setText("Pump");
        btn_pump.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_pumpActionPerformed(evt);
            }
        });
        jPanel1.add(btn_pump, new org.netbeans.lib.awtextra.AbsoluteConstraints(480, 420, 160, 40));

        jLabel4.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel4.setText("Enter Pump Amount");
        jPanel1.add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(480, 280, 140, 20));

        jLabel5.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel5.setText("Select Fuel Type");
        jPanel1.add(jLabel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 110, 130, 20));

        btn_back.setBackground(new java.awt.Color(178, 0, 0));
        btn_back.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btn_back.setForeground(new java.awt.Color(255, 255, 255));
        btn_back.setText("Back");
        btn_back.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_backActionPerformed(evt);
            }
        });
        jPanel1.add(btn_back, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 420, 160, 40));

        btn_clr.setBackground(new java.awt.Color(178, 0, 0));
        btn_clr.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btn_clr.setForeground(new java.awt.Color(255, 255, 255));
        btn_clr.setText("Clear");
        btn_clr.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_clrActionPerformed(evt);
            }
        });
        jPanel1.add(btn_clr, new org.netbeans.lib.awtextra.AbsoluteConstraints(250, 420, 160, 40));
        jPanel1.add(jSeparator1, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 90, 640, 10));

        jLabel6.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel6.setText("Maximum Pump Amount");
        jPanel1.add(jLabel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(370, 110, 150, 30));

        jLabel7.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel7.setText("Minimum Pump Amount");
        jPanel1.add(jLabel7, new org.netbeans.lib.awtextra.AbsoluteConstraints(370, 140, 150, 30));

        jLabel8.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel8.setText("Average Pump Amount");
        jPanel1.add(jLabel8, new org.netbeans.lib.awtextra.AbsoluteConstraints(370, 170, 150, 30));

        maxpump_txt.setEditable(false);
        maxpump_txt.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jPanel1.add(maxpump_txt, new org.netbeans.lib.awtextra.AbsoluteConstraints(530, 110, 110, -1));

        minpump_txt.setEditable(false);
        minpump_txt.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jPanel1.add(minpump_txt, new org.netbeans.lib.awtextra.AbsoluteConstraints(530, 140, 110, -1));

        avaragepump_txt.setEditable(false);
        avaragepump_txt.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jPanel1.add(avaragepump_txt, new org.netbeans.lib.awtextra.AbsoluteConstraints(530, 170, 110, -1));

        pump_valuse_of_date.setEditable(false);
        pump_valuse_of_date.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        pump_valuse_of_date.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pump_valuse_of_dateActionPerformed(evt);
            }
        });
        jPanel1.add(pump_valuse_of_date, new org.netbeans.lib.awtextra.AbsoluteConstraints(160, 270, 150, -1));

        btn_get_pump_valuse_by_date.setBackground(new java.awt.Color(178, 0, 0));
        btn_get_pump_valuse_by_date.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btn_get_pump_valuse_by_date.setForeground(new java.awt.Color(255, 255, 255));
        btn_get_pump_valuse_by_date.setText("Search");
        btn_get_pump_valuse_by_date.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_get_pump_valuse_by_dateActionPerformed(evt);
            }
        });
        jPanel1.add(btn_get_pump_valuse_by_date, new org.netbeans.lib.awtextra.AbsoluteConstraints(220, 330, 90, 30));
        jPanel1.add(jDateChooser_from_date, new org.netbeans.lib.awtextra.AbsoluteConstraints(160, 210, 170, -1));

        money_amount_of_fuel.setEditable(false);
        money_amount_of_fuel.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jPanel1.add(money_amount_of_fuel, new org.netbeans.lib.awtextra.AbsoluteConstraints(160, 300, 150, -1));

        jLabel9.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel9.setText("To Date");
        jPanel1.add(jLabel9, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 240, 90, 30));

        jLabel11.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel11.setText("Pump Amount");
        jPanel1.add(jLabel11, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 270, -1, 30));

        jLabel12.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel12.setText("Pump amount value");
        jPanel1.add(jLabel12, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 300, -1, 20));

        jPanel4.setBackground(new java.awt.Color(234, 234, 234));
        jPanel1.add(jPanel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(360, 100, 290, 110));
        jPanel1.add(jSeparator2, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 400, 630, 10));
        jPanel1.add(jDateChooser_to_date, new org.netbeans.lib.awtextra.AbsoluteConstraints(160, 240, 170, -1));

        jLabel10.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel10.setText("From Date");
        jPanel1.add(jLabel10, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 210, 90, 30));

        jPanel6.setBackground(new java.awt.Color(234, 234, 234));
        jPanel1.add(jPanel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 200, 320, 180));

        getContentPane().add(jPanel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 660, 500));

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void FuelType_Combo_for_calculateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_FuelType_Combo_for_calculateActionPerformed
        String fuelType = FuelType_Combo_for_calculate.getSelectedItem().toString();
        calculatePumpStats(fuelType);
    }//GEN-LAST:event_FuelType_Combo_for_calculateActionPerformed

    private void btn_pumpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_pumpActionPerformed
         try {
            String fuelType = FuelType_Combo_for_calculate.getSelectedItem().toString();
            int amount = Integer.parseInt(pump_text.getText());

            transactionList.addTransaction(fuelType, amount);
            JOptionPane.showMessageDialog(this, "Fuel pumped successfully!");

            transactionList.displayTransactions();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid amount.", "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btn_pumpActionPerformed

    private void pump_textActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pump_textActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_pump_textActionPerformed

    private void btn_backActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_backActionPerformed
        new HomePage().setVisible(true);
        this.dispose();
    }//GEN-LAST:event_btn_backActionPerformed

    private void btn_clrActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_clrActionPerformed
       pump_text.setText("");
       jDateChooser_from_date.setDate(null);
       jDateChooser_to_date.setDate(null);
    }//GEN-LAST:event_btn_clrActionPerformed

    private void pump_valuse_of_dateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pump_valuse_of_dateActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_pump_valuse_of_dateActionPerformed

    private void btn_get_pump_valuse_by_dateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_get_pump_valuse_by_dateActionPerformed
    // Get the dates from the date pickers
    SimpleDateFormat dformat = new SimpleDateFormat("yyyy-MM-dd");

    if (jDateChooser_from_date.getDate() == null || jDateChooser_to_date.getDate() == null) {
        JOptionPane.showMessageDialog(null, "Please select both From and To dates before proceeding.", "Input Error", JOptionPane.WARNING_MESSAGE);
        return; // Stop execution if dates are not selected
    }

    String fromDate = dformat.format(jDateChooser_from_date.getDate()); // Convert From Date to String
    String toDate = dformat.format(jDateChooser_to_date.getDate());     // Convert To Date to String

    // Get the selected fuel type
    String fuelType = FuelType_Combo_for_calculate.getSelectedItem() != null 
                      ? FuelType_Combo_for_calculate.getSelectedItem().toString() 
                      : "";

    if (fuelType.isEmpty()) {
        JOptionPane.showMessageDialog(null, "Please select a fuel type.", "Input Error", JOptionPane.WARNING_MESSAGE);
        return; // Stop execution if no fuel type is selected
    }

    String pumpTable = fuelType.equals("Petrol") ? "petrolpump" : "dieselpump";

    // Get the price for the selected fuel type
    double price = fuelType.equals("Petrol") 
                   ? Double.parseDouble(Pprice_txt.getText()) 
                   : Double.parseDouble(Dprice_txt.getText());

    // SQL query to fetch pump values for the selected date range
    String query = "SELECT SUM(amount) AS total_amount FROM " + pumpTable 
                 + " WHERE DATE(datetime) BETWEEN ? AND ?";

    try (Connection con = DBConnection.getdbconnection();
         PreparedStatement stmt = con.prepareStatement(query)) {
        
        stmt.setString(1, fromDate); // Set From Date in the query
        stmt.setString(2, toDate);   // Set To Date in the query

        try (ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                double totalAmount = rs.getDouble("total_amount");

                // Calculate the total money for the pumped amount
                double totalMoney = price * totalAmount;

                // Set the values in the text fields
                if (totalAmount > 0) {
                    pump_valuse_of_date.setText(totalAmount + "L");
                    money_amount_of_fuel.setText("LKR: " + totalMoney);  // Set the total money in the text field
                } else {
                    JOptionPane.showMessageDialog(null, "No records found for the selected date range.", "Information", JOptionPane.INFORMATION_MESSAGE);
                    pump_valuse_of_date.setText("");  // Clear fields if no records found
                    money_amount_of_fuel.setText("");
                }
            }
        }

    } catch (SQLException e) {
        JOptionPane.showMessageDialog(null, "Error fetching pump data: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
    }

    }//GEN-LAST:event_btn_get_pump_valuse_by_dateActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
     
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new FuelPumpInterface().setVisible(true);
            }
        });
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new FuelPumpInterface().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField Dprice_txt;
    private javax.swing.JComboBox<String> FuelType_Combo_for_calculate;
    private javax.swing.JTextField Pprice_txt;
    private javax.swing.JTextField avaragepump_txt;
    private javax.swing.JButton btn_back;
    private javax.swing.JButton btn_clr;
    private javax.swing.JButton btn_get_pump_valuse_by_date;
    private javax.swing.JButton btn_pump;
    private com.toedter.calendar.JDateChooser jDateChooser_from_date;
    private com.toedter.calendar.JDateChooser jDateChooser_to_date;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JTextField maxpump_txt;
    private javax.swing.JTextField minpump_txt;
    private javax.swing.JTextField money_amount_of_fuel;
    private javax.swing.JTextField pump_text;
    private javax.swing.JTextField pump_valuse_of_date;
    // End of variables declaration//GEN-END:variables
}
