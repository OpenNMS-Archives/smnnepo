# The version used to be passed from build.xml. It's hardcoded here
# the build system generally passes --define "version X" to rpmbuild.
%{!?version:%define version 19.0.0}
# The release number is set to 0 unless overridden
%{!?releasenumber:%define releasenumber 0}
# The install prefix becomes $OPENMS_HOME in the finished package
%{!?instprefix:%define instprefix /opt/minion}
# I think this is the directory where the package will be built
%{!?packagedir:%define packagedir minion-%version-%{releasenumber}}
# Where OpenNMS binaries live
%{!?bindir:%define bindir %instprefix/bin}
# Where the Minion Maven repository WAR should go
%{!?webappdir:%define webappdir /opt/opennms/jetty-webapps}

%{!?jdk:%define jdk java-1.8.0}

%{!?karaf_version:%define karaf_version 2.4.3}

%{!?extrainfo:%define extrainfo }
%{!?extrainfo2:%define extrainfo2 }

# keep RPM from making an empty debug package
%define debug_package %{nil}
# don't do a bunch of weird redhat post-stuff  :)
%define _use_internal_dependency_generator 0
%define __os_install_post %{nil}
%define __find_requires %{nil}
%define __perl_requires %{nil}
%global _binaries_in_noarch_packages_terminate_build 0
AutoReq: no
AutoProv: no

%define with_tests	0%{nil}
%define with_docs	1%{nil}

#%define repodir %{_tmppath}/m2-repo
%define repodir $HOME/.m2/repository

Name:			opennms-minion
Summary:		OpenNMS Minion
Release:		%releasenumber
Version:		%version
License:		AGPL 3.0
Group:			Applications/System
BuildArch:		noarch

Source:			%{name}-source-%{version}-%{releasenumber}.tar.gz
URL:			http://www.opennms.org/
BuildRoot:		%{_tmppath}/%{name}-%{version}-root

# don't worry about buildrequires, the shell script will bomb quick  =)
#BuildRequires:		%{jdk}
Requires:		%{jdk}

%description
OpenNMS Minion is a container infrastructure for distributed, scalable network
management and monitoring.

%{extrainfo}
%{extrainfo2}

%package -n opennms-webapp-minion
Summary:	OpenNMS Minion Feature Repository
Group:		Applications/System
Requires:	opennms-webapp-jetty >= %{version}-0

%description -n opennms-webapp-minion
A Maven repository that provides Minion dependencies to OpenNMS so
that they don't have to be downloaded over the network.

%prep

#rm -rf "%{repodir}"
tar -xvzf $RPM_SOURCE_DIR/%{name}-source-%{version}-%{release}.tar.gz -C $RPM_BUILD_DIR
%define setupdir %{packagedir}

%setup -D -T -n %setupdir

##############################################################################
# building
##############################################################################

%build
rm -rf $RPM_BUILD_ROOT
DONT_GPRINTIFY="yes, please do not"
export DONT_GPRINTIFY

./compile.pl -DkarafVersion=%{karaf_version} -Dmaven.repo.local="%{repodir}" clean install

wget -c -O "%{_tmppath}/apache-karaf-%{karaf_version}.tar.gz" "http://archive.apache.org/dist/karaf/%{karaf_version}/apache-karaf-%{karaf_version}.tar.gz"

##############################################################################
# installation
##############################################################################

%install
DONT_GPRINTIFY="yes, please do not"
export DONT_GPRINTIFY

PREFIXPREFIX=`dirname "$RPM_BUILD_ROOT%{instprefix}"`

install -d -m 755 "$PREFIXPREFIX"
tar -xvzf "%{_tmppath}"/apache-karaf-%{karaf_version}.tar.gz
mv "apache-karaf-%{karaf_version}" "$RPM_BUILD_ROOT%{instprefix}"
touch "$RPM_BUILD_ROOT%{instprefix}/etc/org.opennms.minion.controller.cfg"

# Replace SSH address and port so that we don't conflict with OpenNMS
sed -i "s#sshPort\s*=\s*8101\$#sshPort=8201#" "$RPM_BUILD_ROOT%{instprefix}/etc/org.apache.karaf.shell.cfg"
sed -i "s#sshHost\s*=\s*0.0.0.0\$#sshHost=127.0.0.1#" "$RPM_BUILD_ROOT%{instprefix}/etc/org.apache.karaf.shell.cfg"

# Replace the RMI ports so that we don't conflict with OpenNMS
sed -i "s#^rmiRegistryPort\s*=.*\$#rmiRegistryPort=1299#" "$RPM_BUILD_ROOT%{instprefix}/etc/org.apache.karaf.management.cfg"
sed -i "s#^rmiRegistryHost\s*=.*\$#rmiRegistryHost=127.0.0.1#" "$RPM_BUILD_ROOT%{instprefix}/etc/org.apache.karaf.management.cfg"
sed -i "s#^rmiServerPort\s*=.*\$#rmiServerPort=45444#" "$RPM_BUILD_ROOT%{instprefix}/etc/org.apache.karaf.management.cfg"
sed -i "s#^rmiServerHost\s*=.*\$#rmiServerHost=127.0.0.1#" "$RPM_BUILD_ROOT%{instprefix}/etc/org.apache.karaf.management.cfg"

# Enable the karaf.delay.console option
sed -i "s#^karaf.delay.console\s*=\s*false\$#karaf.delay.console=true#" "$RPM_BUILD_ROOT%{instprefix}/etc/config.properties"

install -d -m 755 "$RPM_BUILD_ROOT%{webappdir}"/minion
unzip -d "$RPM_BUILD_ROOT%{webappdir}"/minion "sampler-repo-webapp/target"/*.war

install -d -m 755 "$RPM_BUILD_ROOT%{_initrddir}" "$RPM_BUILD_ROOT%{_sysconfdir}/sysconfig"

sed -e 's,@INSTPREFIX@,%{instprefix},g' "minion.init" > "$RPM_BUILD_ROOT%{_initrddir}"/minion
chmod 755 "$RPM_BUILD_ROOT%{_initrddir}"/minion

sed -e 's,@INSTPREFIX@,%{instprefix},g' "minion.sh" > "$RPM_BUILD_ROOT%{instprefix}"/bin/start-minion
chmod 755 "$RPM_BUILD_ROOT%{instprefix}"/bin/start-minion

echo "# The the user name for logging into the opennms REST server"      >  "$RPM_BUILD_ROOT%{_sysconfdir}/sysconfig"/minion
echo "USERNAME=\"admin\""                                                >> "$RPM_BUILD_ROOT%{_sysconfdir}/sysconfig"/minion
echo ""                                                                  >> "$RPM_BUILD_ROOT%{_sysconfdir}/sysconfig"/minion
echo "# The password for logging into the opennms REST server"           >> "$RPM_BUILD_ROOT%{_sysconfdir}/sysconfig"/minion
echo "PASSWORD=\"admin\""                                                >> "$RPM_BUILD_ROOT%{_sysconfdir}/sysconfig"/minion
echo ""                                                                  >> "$RPM_BUILD_ROOT%{_sysconfdir}/sysconfig"/minion
echo "# The root of the OpenNMS install (eg, http://localhost:8980/)"    >> "$RPM_BUILD_ROOT%{_sysconfdir}/sysconfig"/minion
echo "OPENNMS=\"\""                                                      >> "$RPM_BUILD_ROOT%{_sysconfdir}/sysconfig"/minion
echo ""                                                                  >> "$RPM_BUILD_ROOT%{_sysconfdir}/sysconfig"/minion
echo "# The ActiveMQ broker URL (defaults to 61616 on the OpenNMS host)" >> "$RPM_BUILD_ROOT%{_sysconfdir}/sysconfig"/minion
echo "BROKER=\"\""                                                       >> "$RPM_BUILD_ROOT%{_sysconfdir}/sysconfig"/minion
echo ""                                                                  >> "$RPM_BUILD_ROOT%{_sysconfdir}/sysconfig"/minion
echo "# The name of this location (from monitoring-locations.xml)"       >> "$RPM_BUILD_ROOT%{_sysconfdir}/sysconfig"/minion
echo "LOCATION=\"\""                                                     >> "$RPM_BUILD_ROOT%{_sysconfdir}/sysconfig"/minion

find "${RPM_BUILD_ROOT}%{instprefix}" ! -type d \
	| grep -v "${RPM_BUILD_ROOT}%{instprefix}/etc" \
	| grep -v "${RPM_BUILD_ROOT}%{instprefix}/bin" \
	| sed -e "s,${RPM_BUILD_ROOT},," \
	> "%{_tmppath}"/files.main

find "${RPM_BUILD_ROOT}%{webappdir}/minion" ! -type d \
	| grep -v -E '\.karaf$' \
	| sed -e "s,${RPM_BUILD_ROOT},," \
	> "%{_tmppath}"/files.webapp

%clean
# SL 2015-02-27 - Why are we deleting the user's .m2/repository directory? I'm 
# commenting this out.
#rm -rf "$RPM_BUILD_ROOT" "%{repodir}"
rm -rf "$RPM_BUILD_ROOT"

##############################################################################
# file setup
##############################################################################

%files -f %{_tmppath}/files.main
%defattr(664 root root 775)
%attr(775,root,root) %{_initrddir}/minion
%config(noreplace) %{_sysconfdir}/sysconfig/minion
%config %{instprefix}/etc/*
%attr(775,root,root) %{instprefix}/bin/*

%files -n opennms-webapp-minion -f %{_tmppath}/files.webapp
%defattr(664 root root 775)
%config(noreplace) %{webappdir}/minion/*.karaf

%post -p /bin/bash
ROOT_INST="$RPM_INSTALL_PREFIX0"
[ -z "$ROOT_INST"  ] && ROOT_INST="%{instprefix}"

# This code is taken from minion.init
if $ROOT_INST/bin/status >/dev/null; then
  $ROOT_INST/bin/admin list | grep '^\[' | sed 's#[][/]# #g' | while read LINE; do
    INSTANCE=`echo "$LINE" | awk '{print $6}'`
    if [ "$INSTANCE" != "root" ]; then
      $ROOT_INST/bin/admin stop $INSTANCE >/dev/null 2>&1
    fi
  done

  $ROOT_INST/bin/stop >/dev/null 
fi

printf -- "- cleaning up %{instprefix}/data... "
for DATADIR in $ROOT_INST/data; do
  if [ -d "$DATADIR" ]; then
    find "$DATADIR/"* -maxdepth 0 -name tmp -prune -o -print | xargs rm -rf
    find "$DATADIR/tmp/"* -maxdepth 0 -name README -prune -o -print | xargs rm -rf
  fi
done
echo "done"

printf -- "- cleaning up instances... "
rm -rf $ROOT_INST/instances
echo "done"

# Clean up any lockfile
rm -rf $ROOT_INST/lock
