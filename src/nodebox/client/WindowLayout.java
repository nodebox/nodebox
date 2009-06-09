package nodebox.client;

public class WindowLayout {

    private String name;
    private Component topComponent;

    public WindowLayout(String name) {
        this.name = name;
    }

    public Component getTopComponent() {
        return topComponent;
    }

    public void setTopComponent(Component topComponent) {
        this.topComponent = topComponent;
    }

    public void apply(NodeBoxDocument document) {

    }

    public void extract(NodeBoxDocument document) {
        document.getContentPane();
    }

    public class Component {
    }

    public class Panel extends Component {

        private Class panelClass;

        public Panel(Class panelClass) {
            this.panelClass = panelClass;
        }
    }

    public class Split extends Component {
        private Component leftComponent;
        private Component rightComponent;
        private int direction;
        private double position;

        public Split(Component leftComponent, Component rightComponent, int direction, double position) {
            this.leftComponent = leftComponent;
            this.rightComponent = rightComponent;
            this.direction = direction;
            this.position = position;
        }
    }


}
