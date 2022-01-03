/*

This class does not contain all the methods used
when handling complex numbers, only the ones
needed for calculating Julia sets:
    add, modulus, square

*/
public class ComplexNumber {
    public double real;
    public double imaginary;

    // Initialize both parts of number to zero
    // if no input is given
    ComplexNumber()
    {
        real = 0.0;
        imaginary = 0.0;
    }

    //Initialize with user input
    ComplexNumber(double real, double imaginary)
    {
        this.real = real;
        this.imaginary = imaginary;
    }

    // Add two complex numbers
    // Result gets saved to the current complex number
    public void add(ComplexNumber cN)
    {
        this.real = this.real + cN.real;
        this.imaginary = this.imaginary + cN.imaginary;
    }

    // The modulus of a complex number describes the distance from (0,0)
    // Result is always a real number
    public double modulus()
    {
        return Math.sqrt(Math.pow(this.real, 2.0) + Math.pow(this.imaginary, 2.0));
    }

    public void square()
    {
        double temp_real =  Math.pow(this.real, 2.0) - Math.pow(this.imaginary, 2.0);
        double temp_imaginary = this.real * this.imaginary * 2;
        this.real = temp_real;
        this.imaginary = temp_imaginary;
    }



}
