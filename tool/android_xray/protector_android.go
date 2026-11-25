//go:build android

package libv2ray

import (
	"errors"
	"log"
	"sync"
	"syscall"

	"github.com/xtls/xray-core/transport/internet"
	"golang.org/x/sys/unix"
)

// V2RayProtector delegates Android socket exclusion to VpnService.protect.
// The int64 descriptor keeps the gomobile API stable as a Java long.
type V2RayProtector interface {
	Protect(fd int64) bool
}

var (
	protectorMu           sync.RWMutex
	protector             V2RayProtector
	protectorRegisterOnce sync.Once
)

// UseProtector registers the Android VpnService socket protector.
//
// Xray's default system dialer invokes registered controllers for TCP and UDP
// sockets before connect or bind. Keeping the default dialer is important: it
// preserves Xray socket options and transport-specific behavior while routing
// the actual file descriptor through VpnService.protect.
func UseProtector(value V2RayProtector) {
	protectorMu.Lock()
	protector = value
	protectorMu.Unlock()

	protectorRegisterOnce.Do(func() {
		if err := internet.RegisterDialerController(protectSystemSocket); err != nil {
			log.Printf("failed to register Android VPN socket protector: %v", err)
		}
	})
}

func protectSystemSocket(_ string, _ string, rawConn syscall.RawConn) error {
	protectorMu.RLock()
	current := protector
	protectorMu.RUnlock()

	protected := false
	if err := rawConn.Control(func(fd uintptr) {
		if current != nil {
			protected = current.Protect(int64(fd))
		}
		if !protected {
			// Xray logs controller errors and continues dialing. Closing the
			// descriptor here makes a rejected protection request fail closed.
			_ = unix.Close(int(fd))
		}
	}); err != nil {
		return err
	}
	if !protected {
		return errors.New("Android VpnService rejected socket protection")
	}
	return nil
}
