package talzemah.blindglasses;

import java.util.Objects;

public class Result {

    private String name;
    private float score;

    public Result() {
    }

    public Result(String name, float score) {
        this.name = name;
        this.score = score;
    }

    public String getName() {
        return name;
    }

    public float getScore() {
        return score;
    }

    public void setname(String name) {

        this.name = name;
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
        return Objects.equals(name, res.name);
    }

}
