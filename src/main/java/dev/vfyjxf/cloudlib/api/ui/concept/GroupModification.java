package dev.vfyjxf.cloudlib.api.ui.concept;

record GroupModification(

) {

    enum Behavior {
        ATTACH, DELETE, REPLACE
    }
}
