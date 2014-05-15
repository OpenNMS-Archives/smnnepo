#
#  $Id$
#
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

Name:			smnnepo
Summary:		SMNnepO
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

%prep

rm -rf "$RPM_BUILD_DIR"/m2-repo
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

./compile.pl -Dmaven.repo.local="$RPM_BUILD_DIR"/m2-repo clean install
wget -O "$RPM_BUILD_DIR"/karaf.tar.gz "http://apache.mirrors.pair.com/karaf/%{karaf_version}/apache-karaf-%{karaf_version}.tar.gz"

##############################################################################
# installation
##############################################################################

%install
DONT_GPRINTIFY="yes, please do not"
export DONT_GPRINTIFY

mkdir -p "$RPM_BUILD_ROOT%{instprefix}"
rmdir "$RPM_BUILD_ROOT%{instprefix}"
tar -xvzf "$RPM_BUILD_DIR"/karaf.tar.gz
mv apache-karaf-* "$RPM_BUILD_ROOT%{instprefix}"
rm -rf "$RPM_BUILD_DIR"/m2-repo/org/opennms/netmgt/sample/sampler-repo-webapp
rsync -ar "$RPM_BUILD_DIR"/m2-repo/ "$RPM_BUILD_ROOT%{instprefix}/system/"

find "${RPM_BUILD_ROOT}%{instprefix}" ! -type d | \
	grep -v "${RPM_BUILD_ROOT}%{instprefix}/etc" | \
	sed -e "s,${RPM_BUILD_ROOT},," > %{_tmppath}/files.main

%clean
rm -rf "$RPM_BUILD_ROOT" "$RPM_BUILD_DIR"/m2-repo

##############################################################################
# file setup
##############################################################################

%files -f %{_tmppath}/files.main
%defattr(664 root root 775)
%config %{instprefix}/etc/*
