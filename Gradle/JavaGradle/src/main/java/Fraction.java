import java.io.*;
/**
 * Purpose: demonstrate simple Java Fraction class with command line,
 * jdb debugging, and Ant build file.
 *
 * Ser321 Foundations of Distributed Applications
 * see http://pooh.poly.asu.edu/Ser321
 * @author Tim Lindquist Tim.Lindquist@asu.edu
 *         Software Engineering, CIDSE, IAFSE, ASU Poly
 * @version January 2020
 */
public class Fraction {

   private int numerator, denominator;

   public Fraction(){
      numerator = denominator = 0;
   }

   public void print() {
    System.out.print(numerator + "/" + denominator );
   }

   public void setNumerator (int n ){
      numerator = n;
   }

   public void setDenominator (int d) {
      denominator = d;
   }

   public int getDenominator() {
      return denominator;
   }

   public int getNumerator() {
      return numerator;
   }

   public static void main (String args[]) {
      if(args.length != 2) {
         System.out.println("Need 2 args.");
         return;
      }

      try {
         int num = Integer.parseInt(args[0]);
         int denom = Integer.parseInt(args[1]);

         Fraction frac = new Fraction();
         frac.setNumerator(num);
         frac.setDenominator(denom);

         // print debug info
         System.out.println("Numerator: " + frac.getNumerator());
         System.out.println("Denominator: " + frac.getDenominator());

         // print it
         System.out.print("The fraction is: ");
         frac.print();
         System.out.println("");

      }catch(Exception e) {
         e.printStackTrace();
      }
   }
}

