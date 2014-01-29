package blocks

import autopipe.dsl._

object xlib {

    class xlibFunc(_name: String) extends AutoPipeFunction(_name) {
        include("X11/Xlib.h")
        library("X11")
        lpath("/usr/X11/lib")
        external("C")
    }

    // Event masks.
    val NoEventMask                = 0
    val KeyPressMask              = 1 << 0
    val KeyReleaseMask            = 1 << 1
    val ButtonPressMask          = 1 << 2
    val ButtonReleaseMask        = 1 << 3
    val EnterWindowMask          = 1 << 4
    val LeaveWindowMask          = 1 << 5
    val PointerMotionMask        = 1 << 6
    val PointerMotionHintMask  = 1 << 7
    val Button1MotionMask        = 1 << 8
    val Button2MotionMask        = 1 << 9
    val Button3MotionMask        = 1 << 10
    val Button4MotionMask        = 1 << 11
    val Button5MotionMask        = 1 << 12
    val ButtonMotionMask         = 1 << 13
    val KeymapStateMask          = 1 << 14
    val ExposureMask              = 1 << 15
    val VisibilityChangeMask    = 1 << 16
    val StructureNotifyMask     = 1 << 17
    val ResizeRedirectMask      = 1 << 18
    val SubstructureNotifyMask = 1 << 19
    val SubstructureRedirectMask = 1 << 20
    val FocusChangeMask          = 1 << 21
    val PropertyChangeMask      = 1 << 22
    val ColormapChangeMask      = 1 << 23
    val OwnerGrabButtonMask     = 1 << 24

    // Events.
    val KeyPress                    = 2
    val KeyRelease                 = 3
    val ButtonPress                = 4
    val ButtonRelease             = 5
    val MotionNotify              = 6
    val EnterNotify                = 7
    val LeaveNotify                = 8
    val FocusIn                     = 9
    val FocusOut                    = 10
    val KeymapNotify              = 11
    val Expose                      = 12
    val GraphicsExpose            = 13
    val NoExpose                    = 14
    val VisibilityNotify         = 15
    val CreateNotify              = 16
    val DestroyNotify             = 17
    val UnmapNotify                = 18
    val MapNotify                  = 19
    val MapRequest                 = 20
    val ReparentNotify            = 21
    val ConfigureNotify          = 22
    val ConfigureRequest         = 23
    val GravityNotify             = 24
    val ResizeRequest             = 25
    val CirculateNotify          = 26
    val CirculateRequest         = 27
    val PropertyNotify            = 28
    val SelectionClear            = 29
    val SelectionRequest         = 30
    val SelectionNotify          = 31
    val ColormapNotify            = 32
    val ClientMessage             = 33
    val MappingNotify             = 34
    val GenericEvent              = 35
    val LASTEvent                  = 36

    // Types.
    val XID = UNSIGNED64
    val GC = new AutoPipeNative("GC")
    val DISPLAY = new AutoPipeNative("Display")
    val XEVENT = new AutoPipeNative("XEvent")
    val XPROPERTYEVENT = new AutoPipeNative("XPropertyEvent")
    val XCLIENTMESSAGEEVENT = new AutoPipeNative("XClientMessageEvent")
    val XGCVALUES = new AutoPipeNative("XGCValues")
    val DISPLAYPTR = new AutoPipePointer(DISPLAY)
    val XEVENTPTR = new AutoPipePointer(XEVENT)
    val XPROPERTYEVENTPTR = new AutoPipePointer(XPROPERTYEVENT)
    val XGCVALUESPTR = new AutoPipePointer(XGCVALUES)
    val XIDPTR = new AutoPipePointer(XID)
    val SIGNED32PTR = new AutoPipePointer(SIGNED32)
    val SIGNED64PTR = new AutoPipePointer(SIGNED64)
    val UNSIGNED64PTR = new AutoPipePointer(UNSIGNED64)
    val UNSIGNED8PTR = new AutoPipePointer(UNSIGNED8)
    val UNSIGNED8PTRPTR = new AutoPipePointer(UNSIGNED8PTR)

    val XOpenDisplay = new xlibFunc("XOpenDisplay") {
        argument(STRING)          // display_name
        returns(DISPLAYPTR)
    }

    val XCloseDisplay = new xlibFunc("XCloseDisplay") {
        argument(DISPLAYPTR)     // display
        returns(SIGNED32)
    }

    val XDefaultRootWindow = new xlibFunc("XDefaultRootWindow") {
        argument(DISPLAYPTR)     // display
        returns(XID)
    }

    val XCreateSimpleWindow = new xlibFunc("XCreateSimpleWindow") {
        argument(DISPLAYPTR)     // display
        argument(XID)              // parent
        argument(SIGNED32)        // x
        argument(SIGNED32)        // y
        argument(UNSIGNED32)     // width
        argument(UNSIGNED32)     // height
        argument(UNSIGNED32)     // border_width
        argument(UNSIGNED64)     // border
        argument(UNSIGNED64)     // background
        returns(UNSIGNED32)
    }

    val XMapWindow = new xlibFunc("XMapWindow") {
        argument(DISPLAYPTR)     // display
        argument(XID)              // w
        returns(SIGNED32)
    }

    val XClearWindow = new xlibFunc("XClearWindow") {
        argument(DISPLAYPTR)     // display
        argument(XID)              // w
        returns(SIGNED32)
    }

    val XSelectInput = new xlibFunc("XSelectInput") {
        argument(DISPLAYPTR)     // display
        argument(XID)              // w
        argument(UNSIGNED64)     // event_mask
        returns(SIGNED32)
    }

    val XPending = new xlibFunc("XPending") {
        argument(DISPLAYPTR)
        returns(SIGNED32)
    }

    val XNextEvent = new xlibFunc("XNextEvent") {
        argument(DISPLAYPTR)
        argument(XEVENTPTR)
        returns(SIGNED32)
    }

    val XCreateGC = new xlibFunc("XCreateGC") {
        argument(DISPLAYPTR)     // display
        argument(XID)              // d
        argument(UNSIGNED64)     // valuemask
        argument(XGCVALUESPTR)  // values
        returns(GC)
    }

    val XFreeGC = new xlibFunc("XFreeGC") {
        argument(DISPLAYPTR)     // display
        argument(GC)                // gc
        returns(SIGNED32)
    }

    val XSetForeground = new xlibFunc("XSetForeground") {
        argument(DISPLAYPTR)     // display
        argument(GC)                // gc
        argument(UNSIGNED64)     // foreground
        returns(SIGNED32)
    }

    val XDrawPoint = new xlibFunc("XDrawPoint") {
        argument(DISPLAYPTR)     // display
        argument(XID)              // d
        argument(GC)                // gc
        argument(SIGNED32)        // x
        argument(SIGNED32)        // y
        returns(SIGNED32)
    }

    val XGetWindowProperty = new xlibFunc("XGetWindowProperty") {
        argument(DISPLAYPTR)     // display
        argument(XID)              // w
        argument(XID)              // property
        argument(SIGNED64)        // offset
        argument(SIGNED64)        // length
        argument(BOOL)             // delete
        argument(XID)              // req_type
        argument(XIDPTR)          // actual_type_return
        argument(SIGNED32PTR)    // actual_format_return
        argument(UNSIGNED64PTR) // nitems_return
        argument(UNSIGNED64PTR) // bytes_after_return
        argument(UNSIGNED8PTRPTR)  // prop_return
        returns(SIGNED32)        
    }

    val XChangeWindowProperty = new xlibFunc("XChangeWindowProperty") {
        argument(DISPLAYPTR)     // display
        argument(XID)              // w
        argument(XID)              // property
        argument(XID)              // type
        argument(SIGNED32)        // format
        argument(SIGNED32)        // mode
        argument(UNSIGNED8PTR)  // data
        argument(SIGNED32)        // nelements
        returns(SIGNED32)
    }

    val XInternAtom = new xlibFunc("XInternAtom") {
        argument(DISPLAYPTR)     // display
        argument(STRING)          // atom_name
        argument(BOOL)             // only_if_exists
        returns(XID)
    }

    val XSetWMProtocols = new xlibFunc("XSetWMProtocols") {
        argument(DISPLAYPTR)     // display
        argument(XID)              // w
        argument(XIDPTR)          // protocols
        argument(SIGNED32)        // count
        returns(SIGNED32)
    }

}
