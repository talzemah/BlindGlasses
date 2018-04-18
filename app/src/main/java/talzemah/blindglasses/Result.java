package talzemah.blindglasses;

import java.util.Objects;

public class Result {

    String name;
    float score;

    public Result() {
    }

    public Result(String name, float score) {
        this.name = name;
        this.score = score;
    }

    public String getname() {
        return name;
    }

    public float getscore() {
        return score;
    }

    public void setname(String name) {

        this.name = name;
    }

    public void setscore(float score) {
        this.score = score;
    }

    @Override
    public String toString() {
        return this.name + " " + this.score;
    }

    @Override
    public boolean equals(Object obj) {

        // self check
        if (this == obj)
            return true;

        // null check
        if (obj == null)
            return false;

        // type check and cast
        if (getClass() != obj.getClass())
            return false;

        Result res = (Result) obj;

        // field comparison
        return Objects.equals(name, res.name) && Objects.equals(score, res.score);
    }
}
