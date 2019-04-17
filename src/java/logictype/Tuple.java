/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package logictype;

/**
 * @param <A>
 * @param <B>
 * @param <C>
 *
 * @author Lochie
 */
public class Tuple<A, B, C> {

    volatile A a;
    volatile B b;
    volatile C c;

    public Tuple() {
    }

    public Tuple(A a, B b, C c) {
	this.a = a;
	this.b = b;
	this.c = c;
    }

    public A a() {
	return this.a;
    }

    public B b() {
	return this.b;
    }

    public C c() {
	return this.c;
    }

    public A setA(A a) {
	A oldA = this.a;
	this.a = a;
	return oldA;
    }

    public B setB(B b) {
	B oldB = this.b;
	this.b = b;
	return oldB;
    }

    public C setC(C c) {
	C oldC = this.c;
	this.c = c;
	return oldC;
    }

}
