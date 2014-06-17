# The version used to be passed from build.xml. It's hardcoded here
# the build system generally passes --define "version X" to rpmbuild.
%{!?version:%define version 1.13.0}
# The release number is set to 0 unless overridden
%{!?releasenumber:%define releasenumber 0}
# The install prefix becomes $OPENMS_HOME in the finished package
%{!?instprefix:%define instprefix /opt/smnnepo}
# I think this is the directory where the package will be built
%{!?packagedir:%define packagedir smnnepo-%version-%{releasenumber}}
# Where OpenNMS binaries live
%{!?bindir:%define bindir %instprefix/bin}
# Where the SMNnepO WAR should go
%{!?webappdir:%define webappdir /opt/opennms/jetty-webapps}

%{!?jdk:%define jdk jdk >= 2000:1.6}

%{!?karaf_version:%define karaf_version 2.3.5}

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

Name:			smnnepo
Summary:		The OpenNMS Sampler/SMNnepO
Release:		%releasenumber
Version:		%version
License:		LGPL/GPL
Group:			Applications/System
BuildArch:		noarch

Source:			%{name}-source-%{version}-%{releasenumber}.tar.gz
URL:			http://www.opennms.org/
BuildRoot:		%{_tmppath}/%{name}-%{version}-root

# don't worry about buildrequires, the shell script will bomb quick  =)
BuildRequires:		%{jdk}

%description
This is SMNnepO.

%{extrainfo}
%{extrainfo2}

%package -n opennms-webapp-smnnepo
Summary:	System repository for OpenNMS SMNnepO components.
Group:		Applications/System
Requires:	opennms-webapp-jetty >= %{version}-0

%description -n opennms-webapp-smnnepo
A maven repository that provides SMNnepO dependencies to OpenNMS so
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

wget -c -O "%{_tmppath}/apache-karaf-%{karaf_version}.tar.gz" "http://apache.mirrors.pair.com/karaf/%{karaf_version}/apache-karaf-%{karaf_version}.tar.gz"

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

install -d -m 755 "$RPM_BUILD_ROOT%{webappdir}"/smnnepo
unzip -d "$RPM_BUILD_ROOT%{webappdir}"/smnnepo "sampler-repo-webapp/target"/*.war

install -d -m 755 "$RPM_BUILD_ROOT%{_initrddir}" "$RPM_BUILD_ROOT%{_sysconfdir}/sysconfig"

sed -e 's,@INSTPREFIX@,%{instprefix},g' "smnnepo.init" > "$RPM_BUILD_ROOT%{_initrddir}"/smnnepo
chmod 755 "$RPM_BUILD_ROOT%{_initrddir}"/smnnepo

sed -e 's,@INSTPREFIX@,%{instprefix},g' "smnnepo.sh" > "$RPM_BUILD_ROOT%{instprefix}"/bin/start-smnnepo
chmod 755 "$RPM_BUILD_ROOT%{instprefix}"/bin/start-smnnepo

echo "# The root of the OpenNMS install (eg, http://localhost:8980/)"    >  "$RPM_BUILD_ROOT%{_sysconfdir}/sysconfig"/smnnepo
echo "OPENNMS=\"\""                                                      >> "$RPM_BUILD_ROOT%{_sysconfdir}/sysconfig"/smnnepo
echo ""                                                                  >> "$RPM_BUILD_ROOT%{_sysconfdir}/sysconfig"/smnnepo
echo "# The ActiveMQ broker URL (defaults to 61616 on the OpenNMS host)" >> "$RPM_BUILD_ROOT%{_sysconfdir}/sysconfig"/smnnepo
echo "BROKER=\"\""                                                       >> "$RPM_BUILD_ROOT%{_sysconfdir}/sysconfig"/smnnepo
echo ""                                                                  >> "$RPM_BUILD_ROOT%{_sysconfdir}/sysconfig"/smnnepo
echo "# The name of this location (from monitoring-locations.xml)"       >> "$RPM_BUILD_ROOT%{_sysconfdir}/sysconfig"/smnnepo
echo "LOCATION=\"\""                                                     >> "$RPM_BUILD_ROOT%{_sysconfdir}/sysconfig"/smnnepo

find "${RPM_BUILD_ROOT}%{instprefix}" ! -type d \
	| grep -v "${RPM_BUILD_ROOT}%{instprefix}/etc" \
	| grep -v "${RPM_BUILD_ROOT}%{instprefix}/bin" \
	| sed -e "s,${RPM_BUILD_ROOT},," \
	> "%{_tmppath}"/files.main

find "${RPM_BUILD_ROOT}%{webappdir}/smnnepo" ! -type d \
	| grep -v -E '\.karaf$' \
	| sed -e "s,${RPM_BUILD_ROOT},," \
	> "%{_tmppath}"/files.webapp

%clean
rm -rf "$RPM_BUILD_ROOT" "%{repodir}"

##############################################################################
# file setup
##############################################################################

%files -f %{_tmppath}/files.main
%defattr(664 root root 775)
%attr(775,root,root) %{_initrddir}/smnnepo
%config(noreplace) %{_sysconfdir}/sysconfig/smnnepo
%config %{instprefix}/etc/*
%attr(775,root,root) %{instprefix}/bin/*

%files -n opennms-webapp-smnnepo -f %{_tmppath}/files.webapp
%defattr(664 root root 775)
%config(noreplace) %{webappdir}/smnnepo/*.karaf
