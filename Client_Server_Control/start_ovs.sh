cd openvswitch-1.11.0/;
./configure --with-linux=/lib/modules/`uname -r`/build;
make && make install;
insmod datapath/linux/openvswitch.ko;
ovsdb-server --remote=punix:/usr/local/var/run/openvswitch/db.sock --remote=db:Open_vSwitch,manager_options --private-key=db:SSL,private_key --certificate=db:SSL,certificate --bootstrap-ca-cert=db:SSL,ca_cert --pidfile --detach;
ovs-vsctl --no-wait init;
ovs-vswitchd --pidfile --detach;
