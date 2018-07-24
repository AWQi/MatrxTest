package com.example.dell.matrxtest;

public class Part {
    private int id ;
    private String name;
    private float l ;
    private  float t;
    private  float r;
    private  float b;

    public Part(int id, String name, float l, float t, float r, float b) {
        this.id = id;
        this.name = name;
        this.l = l;
        this.t = t;
        this.r = r;
        this.b = b;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public float getL() {
        return l;
    }

    public float getT() {
        return t;
    }

    public float getR() {
        return r;
    }

    public float getB() {
        return b;
    }

    @Override
    public String toString() {
        return "Part{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", l=" + l +
                ", t=" + t +
                ", r=" + r +
                ", b=" + b +
                '}';
    }
}
