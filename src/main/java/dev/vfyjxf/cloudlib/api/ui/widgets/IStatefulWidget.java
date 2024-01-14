package dev.vfyjxf.cloudlib.api.ui.widgets;

import dev.vfyjxf.cloudlib.api.ui.state.IState;

public interface IStatefulWidget extends IWidget {


    <SELF extends IStatefulWidget> IState<SELF> createState();

}
