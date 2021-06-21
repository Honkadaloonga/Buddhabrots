package com.p5zf2c46j.util;

public class Complex {

    public double x;
    public double y;

    public Complex() {
        this.x = 0;
        this.y = 0;
    }

    public Complex(double x) {
        this.x = x;
        this.y = 0;
    }

    public Complex(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public static Complex fromAngle(double angle) {
        return new Complex(Math.cos(angle), Math.sin(angle));
    }

    public static Complex fromAngle(double angle, double mag) {
        return new Complex(Math.cos(angle), Math.sin(angle)).mult(mag);
    }

    public Complex set(double x, double y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public Complex set(Complex c) {
        x = c.x;
        y = c.y;
        return this;
    }

    public Complex copy() {
        return new Complex(x, y);
    }

    public static double mag(Complex c) {
        return Math.sqrt(c.x*c.x + c.y*c.y);
    }

    public static double magSqr(Complex c) {
        return c.x*c.x + c.y*c.y;
    }

    public static double arg(Complex c) {
        return Math.atan2(c.y, c.x);
    }

    public Complex add(double x) {
        this.x += x;
        return this;
    }

    public Complex add(double x, double y) {
        this.x += x;
        this.y += y;
        return this;
    }

    public Complex add(Complex c) {
        x += c.x;
        y += c.y;
        return this;
    }

    public static Complex add(Complex c, double x) {
        return new Complex(c.x+x, c.y);
    }

    public static Complex add(Complex c, double x, double y) {
        return new Complex(c.x+x, c.y+y);
    }

    public static Complex add(Complex a, Complex b) {
        return new Complex(a.x+b.x, a.y+b.y);
    }

    public Complex sub(double x) {
        this.x -= x;
        return this;
    }

    public Complex sub(double x, double y) {
        this.x -= x;
        this.y -= y;
        return this;
    }

    public Complex sub(Complex c) {
        x -= c.x;
        y -= c.y;
        return this;
    }

    public static Complex sub(Complex c, double x) {
        return new Complex(c.x-x, c.y);
    }

    public static Complex sub(Complex c, double x, double y) {
        return new Complex(c.x-x, c.y-y);
    }

    public static Complex sub(Complex a, Complex b) {
        return new Complex(a.x-b.x, a.y-b.y);
    }

    public Complex mult(double v) {
        x *= v;
        y *= v;
        return this;
    }

    public Complex mult(double x, double y) {
        return set(this.x*x - this.y*y, this.x*y + this.y*x);
    }

    public Complex mult(Complex c) {
        return set(x * c.x - y * c.y, x * c.y + y * c.x);
    }

    public static Complex mult(Complex c, double v) {
        return new Complex(c.x*v, c.y*v);
    }

    public static Complex mult(Complex c, double x, double y) {
        return new Complex(c.x*x - c.y*y, c.x*y + c.y*x);
    }

    public static Complex mult(Complex a, Complex b) {
        return new Complex(a.x*b.x - a.y*b.y, a.x*b.y + a.y*b.x);
    }

    public Complex div(double v) {
        x /= v;
        y /= v;
        return this;
    }

    public Complex div(double x, double y) {
        return set(this.x*x + this.y*y, this.y*x - this.x*y).div(x*x + y*y);
    }

    public Complex div(Complex c) {
        return set(x*c.x + y*c.y, y*c.x - x*c.y).div(c.x*c.x + c.y*c.y);
    }

    public static Complex div(Complex c, double v) {
        return new Complex(c.x/v, c.y/v);
    }

    public static Complex div(Complex c, double x, double y) {
        return new Complex(c.x*x + c.y*y, c.y*x - c.x*y).div(x*x + y*y);
    }

    public static Complex div(Complex a, Complex b) {
        return new Complex(a.x*b.x + a.y*b.y, a.y*b.x - a.x*b.y).div(b.x*b.x + b.y*b.y);
    }

    public static Complex recip(Complex c) {
        return conj(c).div(magSqr(c));
    }

    public Complex pow(double a) {
        return set(pow(this, a));
    }

    public static Complex pow(Complex c, double a) {
        return fromAngle(a * arg(c)).mult(Math.pow(magSqr(c), a*0.5));
    }

    public Complex pow(double a, double b) {
        return set(pow(this, a, b));
    }

    public Complex pow(Complex c) {
        return set(pow(this, c.x, c.y));
    }

    public static Complex pow(Complex c, double a, double b) {
        double r = mag(c);
        double theta = arg(c);
        return fromAngle(a * theta + b * Math.log(r)).mult(r).mult(Math.exp(-b * theta));
    }

    public static Complex pow(Complex a, Complex b) {
        return pow(a, b.x, b.y);
    }

    public static Complex sqr(Complex c) {
        return new Complex(c.x*c.x - c.y*c.y, 2*c.x*c.y);
    }

    public static Complex sqrt(Complex c) {
        if (c.x == 0 && c.y == 0)
            return new Complex();
        if (c.x < 0 && c.y == 0)
            return new Complex(0, Math.sqrt(Math.abs(c.x)));

        double r = mag(c);
        Complex d = c.copy().add(r);
        return d.div(mag(d)).mult(Math.sqrt(r));
    }

    public static Complex conj(Complex c) {
        return new Complex(c.x, -c.y);
    }

    public static Complex abs(Complex c) {
        return new Complex(Math.abs(c.x), Math.abs(c.y));
    }

    public static Complex floor(Complex c) {
        return new Complex(Math.floor(c.x), Math.floor(c.y));
    }

    public static Complex round(Complex c) {
        return new Complex(Math.round(c.x), Math.round(c.y));
    }

    public static Complex ceil(Complex c) {
        return new Complex(Math.ceil(c.x), Math.ceil(c.y));
    }

    public static Complex trunc(Complex c) {
        return new Complex(c.x - Math.floor(c.x), c.y - Math.floor(c.y));
    }

    public static Complex flip(Complex c) {
        return new Complex(c.y, c.x);
    }

    public static Complex log(Complex c) {
        return new Complex(Math.log(mag(c)), arg(c));
    }

    public static Complex exp(Complex c) {
        return fromAngle(c.y).mult(Math.exp(c.x));
    }

    public static Complex sin(Complex c) {
        double q = Math.exp(c.y);
        double qi = 1 / q;
        return new Complex(Math.sin(c.x) * (q + qi), Math.cos(c.x) * (q - qi)).mult(0.5);
    }

    public static Complex sinh(Complex c) {
        return new Complex(Math.sinh(c.x) * Math.cos(c.y), Math.cosh(c.x) * Math.sin(c.y));
    }

    public static Complex asin(Complex c) {
        return log(sqrt(new Complex(1, 0).sub(sqr(c))).add(mult(c, 0, 1))).mult(0, -1);
    }

    public static Complex asinh(Complex c) {
        return log(sqrt(sqr(c).add(1)).add(c));
    }

    public static Complex cos(Complex c) {
        double q = Math.exp(c.y);
        double qi = 1 / q;
        return new Complex(Math.cos(c.x) * (q + qi) * 0.5, -Math.sin(c.x) * (q - qi) * 0.5);
    }

    public static Complex cosh(Complex c) {
        return new Complex(Math.cosh(c.x) * Math.cos(c.y), Math.sinh(c.x) * Math.sin(c.y));
    }

    public static Complex acos(Complex c) {
        return log(c.copy().add(sqrt(sqr(c).sub(1)))).mult(0, -1);
    }

    public static Complex acosh(Complex c) {
        return log(sqrt(sub(c, 1)).mult(sqrt(add(c, 1))).add(c));
    }

    public static Complex tan(Complex c) {
        return div(sin(c), cos(c));
    }

    public static Complex tanh(Complex c) {
        return div(sinh(c), cosh(c));
    }

    public static Complex atan(Complex c) {
        return log(c.copy().mult(0, 1).sub(1).mult(-1).div(c.copy().mult(0, 1).add(1))).mult(0, 0.5);
    }

    public static Complex atanh(Complex c) {
        return log(c.copy().add(1).div(c.copy().sub(1).mult(-1))).mult(0.5);
    }

    public static Complex cot(Complex c) {
        return div(cos(c), sin(c));
    }

    public static Complex coth(Complex c) {
        return div(cosh(c), sinh(c));
    }

    public static Complex acot(Complex c) {
        return log(c.copy().sub(0, 1).div(c.copy().add(0, 1))).mult(0, 0.5);
    }

    public static Complex acoth(Complex c) {
        return log(add(c, 1).div(sub(c, 1))).mult(0.5);
    }

    @Override
    public String toString() {
        return "["+x+", "+y+"]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Complex))
            return false;

        Complex c = (Complex) o;
        return Double.compare(c.y, y) == 0 && (Double.compare(c.x, x) == 0);
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(x);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(y);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}